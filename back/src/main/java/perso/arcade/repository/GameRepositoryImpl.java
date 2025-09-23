package perso.arcade.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import perso.arcade.model.dto.ClassementDto;

import java.util.List;

@Repository
public class GameRepositoryImpl implements GameRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ClassementDto> getLeaderboard(String gameName) {
        String req = """
                    SELECT p.pseudo, r.score
                    FROM Record r
                    JOIN r.player p
                    JOIN r.game g
                    WHERE g.name = :gameName
                    ORDER BY r.score DESC
                """;
        TypedQuery<ClassementDto> query = entityManager.createQuery(req, ClassementDto.class);
        query.setParameter("gameName", gameName);
        return query.getResultList();
    }
}