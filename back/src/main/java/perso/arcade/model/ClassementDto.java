package perso.arcade.model;

public class ClassementDto {
    private String pseudo;
    private Long score;

    public ClassementDto(String pseudo, Long score) {
        this.pseudo = pseudo;
        this.score = score;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public Long getScore() {
        return score;
    }

    public void setScore(Long score) {
        this.score = score;
    }
}
