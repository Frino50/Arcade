package perso.arcade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import perso.arcade.model.Game;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>, GameRepositoryCustom {
    Optional<Game> findByName(String name);
}
