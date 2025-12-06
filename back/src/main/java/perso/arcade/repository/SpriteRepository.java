package perso.arcade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import perso.arcade.model.entities.Sprite;

@Repository
public interface SpriteRepository extends JpaRepository<Sprite, Long>, SpriteRepositoryCustom {
}
