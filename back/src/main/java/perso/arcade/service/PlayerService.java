package perso.arcade.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import perso.arcade.model.CustomUserDetails;
import perso.arcade.model.dto.ConnexionDto;
import perso.arcade.model.entities.Player;
import perso.arcade.repository.PlayerRepository;

@Service
public class PlayerService implements UserDetailsService {

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

    @Override
    public CustomUserDetails loadUserByUsername(String pseudo) throws UsernameNotFoundException {
        Player player = playerRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le pseudo: " + pseudo));
        return new CustomUserDetails(player);
    }
}