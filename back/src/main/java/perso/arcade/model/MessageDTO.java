package perso.arcade.model;

import java.time.LocalDateTime;

public class MessageDTO {
    private Long id;
    private String sender;
    private String content;
    private LocalDateTime timestamp;

    public MessageDTO() {
    }

    public MessageDTO(Long id, String sender, String content, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
