package perso.arcade.repository;

import perso.arcade.model.dto.SpriteSummaryDTO;
import perso.arcade.model.enumeration.AnimationType;

import java.util.List;

public interface SpriteRepositoryCustom {
    List<SpriteSummaryDTO> findAllSpriteSummariesWithIdleAnimation(AnimationType idleType, String idleTypeString);
}
