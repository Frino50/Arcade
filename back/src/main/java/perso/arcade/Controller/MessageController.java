package perso.arcade.Controller;

import org.springframework.web.bind.annotation.*;
import perso.arcade.model.MessageDTO;
import perso.arcade.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/{message}")
    public void sendMessage(@PathVariable String message) {
        messageService.sendMessage(message);
    }

    @GetMapping()
    @ResponseBody
    public List<MessageDTO> getRecent() {
        return messageService.getRecentMessages();
    }
}
