package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    // @EntityGraph: trae las canciones en la misma consulta. El listado /my mapea
    // cada playlist a DTO leyendo getSongs(), que con LAZY producía un N+1.
    @EntityGraph(attributePaths = {"songs"})
    List<Playlist> findByCreator(User creator);
    List<Playlist> findByCreatorAndIsPublicTrue(User creator);
    List<Playlist> findByIsPublicTrue();

    /**
     * Todas las playlists con sus canciones y su creador ya resueltos para la
     * búsqueda en memoria, que filtra por visibilidad (creador) y puntúa por
     * número de canciones. {@code songs} es la única colección del grafo, así
     * que no hay riesgo de {@code MultipleBagFetchException}.
     */
    @EntityGraph(attributePaths = {"songs", "creator"})
    @Query("select p from Playlist p")
    List<Playlist> findAllForSearch();
}
