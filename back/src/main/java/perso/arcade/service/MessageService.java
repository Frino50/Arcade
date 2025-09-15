package perso.arcade.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import perso.arcade.model.Message;
import perso.arcade.model.MessageDTO;
import perso.arcade.model.Player;
import perso.arcade.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UtilService utilService;

    public MessageService(MessageRepository messageRepository, SimpMessagingTemplate messagingTemplate, UtilService utilService) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.utilService = utilService;
    }

    public void sendMessage(String message) {
        Player player = utilService.getPlayer();

        Message msg = new Message();
        msg.setSender(player);
        msg.setContent(message);
        msg.setTimestamp(LocalDateTime.now());
        msg = messageRepository.save(msg);

        messagingTemplate.convertAndSend("/topic/chat",
                new MessageDTO(msg.getId(), player.getPseudo(), msg.getContent(), msg.getTimestamp()));
    }

    public List<MessageDTO> getRecentMessages() {
        return messageRepository.findTop50ByOrderByTimestampAsc()
                .stream()
                .map(m -> new MessageDTO(m.getId(), m.getSender().getPseudo(), m.getContent(), m.getTimestamp()))
                .collect(Collectors.toList());
    }
}
