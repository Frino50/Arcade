package perso.arcade.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import perso.arcade.model.dto.SpriteInfos;
import perso.arcade.model.enumeration.AnimationType;

import java.util.List;

@Repository
public class SpriteRepositoryImpl implements SpriteRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SpriteInfos> getAllSpritesInfos(AnimationType idleType) {

        String req = """
                SELECT new perso.arcade.model.dto.SpriteInfos(
                                s.id,
                                s.name,
                                s.name || '/' || :idleType || '/1.png',
                                a.width,
                                a.height,
                                a.frames,
                                s.scale
                            )
                            FROM Sprite s JOIN s.animations a
                            WHERE a.type = :idleType
                """;
        TypedQuery<SpriteInfos> query = entityManager.createQuery(req, SpriteInfos.class);
        query.setParameter("idleType", idleType);
        return query.getResultList();
    }

    @Override
    public SpriteInfos getSpritesInfosById(AnimationType idleType, Long spriteId) {

        String req = """
                SELECT new perso.arcade.model.dto.SpriteInfos(
                                s.id,
                                s.name,
                                s.name || '/' || :idleType || '/1.png',
                                a.width,
                                a.height,
                                a.frames,
                                s.scale
                            )
                        FROM Sprite s
                        JOIN s.animations a
                        WHERE a.type = :idleType
                          AND s.id = :spriteId
                """;

        TypedQuery<SpriteInfos> query = entityManager.createQuery(req, SpriteInfos.class);
        query.setParameter("idleType", idleType);
        query.setParameter("spriteId", spriteId);

        return query.getSingleResult();
    }

}