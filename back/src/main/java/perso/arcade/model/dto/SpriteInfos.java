package perso.arcade.model.dto;

public class SpriteInfos {
    private final Long animationId;
    private String name;
    private String imageUrl;
    private int width;
    private int height;
    private int frames;
    private float scale;
    private int frameRate;

    public SpriteInfos(Long animationId, String name, String imageUrl, int width, int height, int frames, float scale, int frameRate) {
        this.animationId = animationId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.scale = scale;
        this.frameRate = frameRate;
    }

    public Long getAnimationId() {
        return animationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }
}