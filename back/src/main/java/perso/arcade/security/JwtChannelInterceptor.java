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
import java.util.Objects;
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
        String sessionId = accessor.getSessionId();
        StompCommand command = Objects.requireNonNull(accessor.getCommand());
        switch (command) {
            case CONNECT -> handleConnect(accessor, sessionId);
            case DISCONNECT -> handleDisconnect(sessionId);
            default -> {
                if (!setPrincipalIfExists(accessor, sessionId)) {
                    System.out.println("Commande STOMP " + command + " bloquée : utilisateur non authentifié");
                    return null;
                }
                logCommand(accessor);
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor, String sessionId) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            if (jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = playerService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                accessor.setUser(authentication);
                sessionUserMap.put(sessionId, authentication);

                System.out.println("STOMP CONNECT de l'utilisateur : " + username);
                return;
            }
        }
        System.out.println("STOMP CONNECT refusé : JWT invalide ou absent");
    }

    private void handleDisconnect(String sessionId) {
        Principal user = sessionUserMap.remove(sessionId);
        if (user != null) {
            System.out.println("Session STOMP déconnectée pour l'utilisateur : " + user.getName());
        }
    }

    private boolean setPrincipalIfExists(StompHeaderAccessor accessor, String sessionId) {
        Principal user = accessor.getUser();
        if (user == null) {
            user = sessionUserMap.get(sessionId);
            if (user != null) {
                accessor.setUser(user);
            }
        }
        return user != null;
    }

    private void logCommand(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user != null) {
            System.out.println("STOMP " + accessor.getCommand() + " de l'utilisateur : " + user.getName());
        }
    }

    public String getPseudo(String sessionId) {
        Principal principal = sessionUserMap.get(sessionId);
        if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
            Object userDetailsObj = authToken.getPrincipal();
            if (userDetailsObj instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            } else {
                return authToken.getName();
            }
        } else if (principal != null) {
            return principal.getName();
        } else {
            return null;
        }
    }
}
