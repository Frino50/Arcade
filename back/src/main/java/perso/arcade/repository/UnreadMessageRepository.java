package perso.arcade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import perso.arcade.model.UnreadMessage;

import java.util.List;

public interface UnreadMessageRepository extends JpaRepository<UnreadMessage, Long> {
    List<UnreadMessage> findByReceiver_Id(Long receiverId);

    List<UnreadMessage> findByReceiver_pseudo(String pseudo);

    void deleteByReceiver_IdAndMessage_Id(Long receiverId, Long messageId);

    void deleteByReceiver_pseudo(String pseudo);
}