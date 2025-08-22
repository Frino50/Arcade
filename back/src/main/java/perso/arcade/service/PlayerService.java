package perso.arcade.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import perso.arcade.model.ConnexionDto;
import perso.arcade.model.Player;
import perso.arcade.repository.PlayerRepository;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player register(ConnexionDto connexionDto) {
        if (playerRepository.findByPseudo(connexionDto.getPseudo()).isPresent()) {
            throw new RuntimeException("Pseudo déjà utilisé");
        }
        String hashedPassword = passwordEncoder.encode(connexionDto.getPassword());
        Player player = new Player(null, connexionDto.getPseudo(), hashedPassword);
        return playerRepository.save(player);
    }

    public boolean login(ConnexionDto connexionDto) {
        return playerRepository.findByPseudo(connexionDto.getPseudo())
                .map(player -> passwordEncoder.matches(connexionDto.getPassword(), player.getPassword()))
                .orElse(false);
    }
}

