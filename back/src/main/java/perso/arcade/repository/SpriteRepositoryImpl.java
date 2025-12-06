package perso.arcade.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import perso.arcade.model.dto.SpriteSummaryDTO;
import perso.arcade.model.enumeration.AnimationType;

import java.util.List;

@Repository
public class SpriteRepositoryImpl implements SpriteRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SpriteSummaryDTO> findAllSpriteSummariesWithIdleAnimation(AnimationType idleType, String idleTypeString) {

        String req = """
                SELECT new perso.arcade.model.dto.SpriteSummaryDTO(
                                s.id,
                                s.name,
                                'http://localhost:8085/sprites/' || s.name || '/' || :idleTypeString || '/1.png',
                                a.width,
                                a.height,
                                a.frames
                            )
                            FROM Sprite s JOIN s.animations a
                            WHERE a.type = :idleType
                """;
        TypedQuery<SpriteSummaryDTO> query = entityManager.createQuery(req, SpriteSummaryDTO.class);
        query.setParameter("idleTypeString", idleTypeString);
        query.setParameter("idleType", idleType);
        return query.getResultList();
    }
}
