package perso.arcade.Controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import perso.arcade.model.ConnexionDto;
import perso.arcade.model.Player;
import perso.arcade.service.PlayerService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PlayerService playerService;

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    public Player register(@RequestBody ConnexionDto connexionDto) {
        return playerService.register(connexionDto);
    }

    @PostMapping("/login")
    public boolean login(@RequestBody ConnexionDto connexionDto) {
        return playerService.login(connexionDto);
    }
}

