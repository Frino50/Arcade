package perso.arcade.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import perso.arcade.service.PlayerService;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final PlayerService playerService;
    private final Map<String, Principal> sessionUserMap = new ConcurrentHashMap<>();

    public JwtChannelInterceptor(JwtUtils jwtUtils, PlayerService playerService) {
        this.jwtUtils = jwtUtils;
        this.playerService = playerService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (command == null) {
            return message;
        }

        String sessionId = accessor.getSessionId();

        switch (command) {
            case CONNECT -> processConnect(accessor, sessionId);
            case DISCONNECT -> processDisconnect(sessionId);
            default -> {
                if (!ensureUserAuthentication(accessor, sessionId)) {
                    System.out.println("Commande STOMP " + command + " bloquée : utilisateur non authentifié");
                    return null;
                }
                logCommand(accessor);
            }
        }
        return message;
    }

    private void processConnect(StompHeaderAccessor accessor, String sessionId) {
        String jwt = extractJwt(accessor);

        if (jwt == null || !jwtUtils.validateJwtToken(jwt)) {
            System.out.println("STOMP CONNECT refusé : JWT invalide ou absent");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = buildAuthentication(jwt);
        accessor.setUser(authentication);
        sessionUserMap.put(sessionId, authentication);

        System.out.println("STOMP CONNECT de l'utilisateur : " + authentication.getName());
    }

    private void processDisconnect(String sessionId) {
        Principal user = sessionUserMap.remove(sessionId);
        if (user != null) {
            System.out.println("Session STOMP déconnectée pour l'utilisateur : " + user.getName());
        }
    }

    private boolean ensureUserAuthentication(StompHeaderAccessor accessor, String sessionId) {
        Principal user = accessor.getUser();

        if (user == null) {
            user = sessionUserMap.get(sessionId);
            if (user != null) {
                accessor.setUser(user);
            }
        }

        return user != null;
    }

    private String extractJwt(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(String jwt) {
        String username = jwtUtils.getUserNameFromJwtToken(jwt);
        UserDetails userDetails = playerService.loadUserByUsername(username);

        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
    }

    private void logCommand(StompHeaderAccessor accessor) {
        String jwt = extractJwt(accessor);

        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            System.out.println("STOMP " + accessor.getCommand() + " de l'utilisateur : " + username + " sur " + accessor.getDestination());
        }
    }
}