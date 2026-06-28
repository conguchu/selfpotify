package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Album;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository extends JpaRepository<Album, Long> {

    Optional<Album> findByNameIgnoreCase(String name);

    /**
     * Todos los álbumes con sus artistas ya resueltos para la búsqueda en
     * memoria. La colección {@code songs} se deja fuera del grafo —no puede ir
     * junto a {@code artists} sin provocar {@code MultipleBagFetchException}— y
     * se trae por lotes vía el {@code @BatchSize} declarado en {@link
     * Album#getSongs()}.
     */
    @EntityGraph(attributePaths = {"artists"})
    @Query("select a from Album a")
    List<Album> findAllForSearch();
}
