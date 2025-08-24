package perso.arcade.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import perso.arcade.model.Game;
import perso.arcade.model.Player;
import perso.arcade.model.Record;

import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    Optional<Record> findByPlayerAndGame(Player player, Game game);

}
