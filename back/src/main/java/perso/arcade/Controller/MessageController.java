package perso.arcade.Controller;

import org.springframework.web.bind.annotation.*;
import perso.arcade.model.PageResponse;
import perso.arcade.model.dto.MessageDto;
import perso.arcade.service.MessageService;

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
    public PageResponse<MessageDto> getMessages(@RequestParam int page, @RequestParam int size) {
        return new PageResponse<>(messageService.getMessages(page, size));
    }
}
