package perso.arcade.model.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "lower_is_better", nullable = false)
    private boolean lowerIsBetter = false;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Record> records;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Record> getRecords() {
        return records;
    }

    public void setRecords(Set<Record> records) {
        this.records = records;
    }

    public boolean isLowerIsBetter() {
        return lowerIsBetter;
    }

    public void setLowerIsBetter(boolean lowerIsBetter) {
        this.lowerIsBetter = lowerIsBetter;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return lowerIsBetter == game.lowerIsBetter && Objects.equals(id, game.id) && Objects.equals(name, game.name) && Objects.equals(records, game.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, lowerIsBetter, records);
    }
}