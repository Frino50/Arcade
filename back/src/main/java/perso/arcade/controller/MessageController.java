package perso.arcade.controller;

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

    @GetMapping("/{page}/{size}")
    public PageResponse<MessageDto> getMessages(@PathVariable int page, @PathVariable int size) {
        return new PageResponse<>(messageService.getMessages(page, size));
    }
}