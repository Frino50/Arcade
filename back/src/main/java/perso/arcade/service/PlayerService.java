package perso.arcade.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import perso.arcade.model.CustomUserDetails;
import perso.arcade.model.entities.Player;
import perso.arcade.repository.PlayerRepository;

@Service
public class PlayerService implements UserDetailsService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public CustomUserDetails loadUserByUsername(String pseudo) throws UsernameNotFoundException {
        Player player = playerRepository.findByPseudo(pseudo)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le pseudo: " + pseudo));
        return new CustomUserDetails(player);
    }
}