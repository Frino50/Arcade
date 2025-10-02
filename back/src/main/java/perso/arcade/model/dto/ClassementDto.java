package perso.arcade.model.dto;

public class ClassementDto {
    private String pseudo;
    private String score;

    public ClassementDto(String pseudo, String score) {
        this.pseudo = pseudo;
        this.score = score;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }
}
