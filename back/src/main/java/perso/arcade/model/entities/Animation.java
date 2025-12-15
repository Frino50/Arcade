package perso.arcade.model.entities;

import jakarta.persistence.*;
import perso.arcade.model.enumeration.AnimationType;

import java.util.Objects;

@Entity
public class Animation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frames", nullable = false)
    private int frames;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "indice", nullable = false)
    private int indice;


    @Enumerated(EnumType.STRING)
    private AnimationType type;

    @ManyToOne
    @JoinColumn(name = "sprite_id")
    private Sprite sprite;

    public Animation() {
    }

    public Animation(int frames, int width, int height, AnimationType type, int indice) {
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.type = type;
        this.indice = indice;
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

    public int getIndice() {
        return indice;
    }

    public void setIndice(int indice) {
        this.indice = indice;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Animation animation = (Animation) o;
        return frames == animation.frames && width == animation.width && height == animation.height && indice == animation.indice && Objects.equals(id, animation.id) && type == animation.type && Objects.equals(sprite, animation.sprite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, frames, width, height, indice, type, sprite);
    }
}
