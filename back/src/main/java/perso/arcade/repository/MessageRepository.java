package perso.arcade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import perso.arcade.model.entities.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop50ByOrderByTimestampAsc();
}