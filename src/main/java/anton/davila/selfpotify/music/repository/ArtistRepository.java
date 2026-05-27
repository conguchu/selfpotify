package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Artist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByNameIgnoreCase(String name);

    Optional<Artist> findByMbid(String mbid);

    /**
     * Artistas que tienen al menos una canción de un género, según el catálogo
     * (no las escuchas). Permite ampliar el feed con artistas afines a los
     * géneros del usuario aunque todavía no tengan ninguna escucha registrada.
     */
    @Query("select distinct a from Artist a join a.songs s where s.genre = :genre")
    List<Artist> findArtistsByGenre(@Param("genre") String genre, Pageable pageable);
}
