package perso.arcade.repository;

import perso.arcade.model.ClassementDto;

import java.util.List;

public interface GameRepositoryCustom {
    List<ClassementDto> getLeaderboard(String gameName);
}
