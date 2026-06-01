package anton.davila.selfpotify.user.feed.repository;

import anton.davila.selfpotify.user.feed.entity.UserFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFeedRepository extends JpaRepository<UserFeed, Long> {

    /**
     * Feeds que recomiendan al artista dado. Se usa al borrar/unir/separar un
     * artista para soltar la FK de la tabla cruzada {@code recommendedArtists}
     * antes de eliminar la fila del artista. El feed se regenera en el siguiente
     * acceso al home, así que basta con quitar la referencia.
     */
    @Query("select f from UserFeed f join f.recommendedArtists a where a.id = :artistId")
    List<UserFeed> findAllByRecommendedArtistId(@Param("artistId") Long artistId);
}
