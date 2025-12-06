package perso.arcade.model.entities;

import jakarta.persistence.*;
import perso.arcade.model.enumeration.AnimationType;

import java.util.Objects;

@Entity
public class Animation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int frames;
    private int width;
    private int height;

    @Enumerated(EnumType.STRING)
    private AnimationType type;

    @ManyToOne
    @JoinColumn(name = "sprite_id")
    private Sprite sprite;

    public Animation() {
    }

    public Animation(int frames, int width, int height, AnimationType type) {
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public int getFrames() {
        return frames;
    }

    public void setFrames(int frames) {
        this.frames = frames;
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

    public AnimationType getType() {
        return type;
    }

    public void setType(AnimationType type) {
        this.type = type;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Animation animation = (Animation) o;
        return frames == animation.frames && width == animation.width && height == animation.height && Objects.equals(id, animation.id) && Objects.equals(type, animation.type) && Objects.equals(sprite, animation.sprite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, frames, width, height, type, sprite);
    }
}
