package perso.arcade.model.dto;

public class SpriteSummaryDTO {
    private final Long id;
    private String name;
    private String idleImageUrl;
    private int width;
    private int height;
    private int frames;

    public SpriteSummaryDTO(Long id, String name, String idleImageUrl, int width, int height, int frames) {
        this.id = id;
        this.name = name;
        this.idleImageUrl = idleImageUrl;
        this.width = width;
        this.height = height;
        this.frames = frames;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdleImageUrl() {
        return idleImageUrl;
    }

    public void setIdleImageUrl(String idleImageUrl) {
        this.idleImageUrl = idleImageUrl;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
    }
}