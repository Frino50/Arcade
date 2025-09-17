package perso.arcade.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import perso.arcade.model.dto.MessageDto;
import perso.arcade.model.entities.Message;
import perso.arcade.model.entities.Player;
import perso.arcade.repository.MessageRepository;

import java.time.LocalDateTime;

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
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide ou ne contenir que des espaces.");
        }

        Player player = utilsService.getPlayer();

        Message msg = new Message();
        msg.setPlayer(player);
        msg.setContent(message);
        msg.setTimestamp(LocalDateTime.now());
        msg = messageRepository.save(msg);

        messagingTemplate.convertAndSend("/topic/chat",
                new MessageDto(msg.getId(), player.getPseudo(), msg.getContent(), msg.getTimestamp()));
    }


    public Page<MessageDto> getMessages(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return messageRepository.findAllMessages(pageable);
    }

}
