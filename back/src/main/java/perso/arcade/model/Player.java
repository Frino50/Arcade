package perso.arcade.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "player")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String pseudo;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Record> records;

    public Player() {
    }

    public Player(Long id, String pseudo, String hashedPassword) {
        this.id = id;
        this.pseudo = pseudo;
        this.password = hashedPassword;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public Set<Record> getRecords() {
        return records;
    }

    public void setRecords(Set<Record> records) {
        this.records = records;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id) && Objects.equals(pseudo, player.pseudo) && Objects.equals(password, player.password) && Objects.equals(records, player.records);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, pseudo, password, records);
    }
}