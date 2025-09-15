package perso.arcade.Controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import perso.arcade.model.dto.ConnexionDto;
import perso.arcade.model.dto.LoginResponseDto;
import perso.arcade.model.entities.Player;
import perso.arcade.security.JwtUtils;
import perso.arcade.service.PlayerService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PlayerService playerService;

    private final AuthenticationManager authenticationManager;

    private final JwtUtils jwtUtils;


    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, PlayerService playerService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.playerService = playerService;
    }

    @PostMapping("/register")
    public Player register(@RequestBody ConnexionDto connexionDto) {
        return playerService.register(connexionDto);
    }

    @PostMapping("/login")
    public LoginResponseDto authenticateUser(@RequestBody ConnexionDto connexionDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(connexionDto.getPseudo(), connexionDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new LoginResponseDto(jwt, connexionDto.getPseudo());
    }
}

