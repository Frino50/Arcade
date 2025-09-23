package perso.arcade.Controller;

import org.springframework.web.bind.annotation.*;
import perso.arcade.model.PageResponse;
import perso.arcade.model.dto.MessageDto;
import perso.arcade.service.MessageService;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping()
    public void sendMessage(@RequestBody Map<String, String> data) {
        messageService.sendMessage(data.get("message"));
    }

    @GetMapping()
    public PageResponse<MessageDto> getMessages(@RequestParam int page, @RequestParam int size) {
        return new PageResponse<>(messageService.getMessages(page, size));
    }
}