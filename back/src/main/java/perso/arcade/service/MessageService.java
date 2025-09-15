package perso.arcade.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import perso.arcade.model.dto.MessageDto;
import perso.arcade.model.entities.Message;
import perso.arcade.model.entities.Player;
import perso.arcade.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UtilsService utilsService;

    public MessageService(MessageRepository messageRepository, SimpMessagingTemplate messagingTemplate, UtilsService utilsService) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.utilsService = utilsService;
    }

    public void sendMessage(String message) {
        Player player = utilsService.getPlayer();

        Message msg = new Message();
        msg.setPlayer(player);
        msg.setContent(message);
        msg.setTimestamp(LocalDateTime.now());
        msg = messageRepository.save(msg);

        messagingTemplate.convertAndSend("/topic/chat",
                new MessageDto(msg.getId(), player.getPseudo(), msg.getContent(), msg.getTimestamp()));
    }

    public List<MessageDto> getRecentMessages() {
        return messageRepository.findTop50ByOrderByTimestampAsc()
                .stream()
                .map(m -> new MessageDto(m.getId(), m.getPlayer().getPseudo(), m.getContent(), m.getTimestamp()))
                .collect(Collectors.toList());
    }
}
