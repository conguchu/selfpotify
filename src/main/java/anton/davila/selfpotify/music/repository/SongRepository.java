package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findFirstBySongPath(String songPath);

    /**
     * Todas las canciones con sus artistas y álbum ya resueltos en la misma
     * consulta. Pensado para la búsqueda en memoria del {@link
     * anton.davila.selfpotify.music.service.SearchService}, que toca esas dos
     * relaciones por cada fila: cargarlas con un {@link EntityGraph} colapsa el
     * N+1 (una consulta por canción) en una sola. {@code artists} es la única
     * colección del grafo para no incurrir en {@code MultipleBagFetchException}.
     */
    @EntityGraph(attributePaths = {"artists", "album"})
    @Query("select s from Song s")
    List<Song> findAllForSearch();

    /**
     * Número de canciones por género (ignora nulos y vacíos), agregado en SQL.
     * Evita cargar todo el catálogo en memoria solo para contar, como hacía la
     * búsqueda de géneros. Cada fila es {@code [genre, total]}.
     */
    @Query("select s.genre, count(s) from Song s "
            + "where s.genre is not null and s.genre <> '' group by s.genre")
    List<Object[]> countSongsByGenre();

    // =====================================================================
    // ----- IDs para los "descubrimientos diarios" (ver DailyDiscoveryService)
    // -----  Devuelven IDs ordenados por id: una base determinista sobre la
    // -----  que el servicio muestrea/baraja con un Random sembrado por día,
    // -----  de modo que el resultado es estable durante toda la jornada.
    // =====================================================================

    /** IDs de todas las canciones disponibles, orden estable por id. */
    @Query("select s.id from Song s where s.available = true order by s.id")
    List<Long> findAvailableSongIds();

    /** IDs de las canciones disponibles de un género, orden estable por id. */
    @Query("select s.id from Song s where s.available = true and s.genre = :genre order by s.id")
    List<Long> findAvailableSongIdsByGenre(@Param("genre") String genre);

    /**
     * IDs de las canciones disponibles de un género que el usuario aún NO ha
     * escuchado (no aparecen en sus filas de {@code user_song_listen}).
     */
    @Query("select s.id from Song s where s.available = true and s.genre = :genre "
            + "and s.id not in (select e.song.id from UserSongListen e where e.user.id = :userId) "
            + "order by s.id")
    List<Long> findUnheardSongIdsByGenre(@Param("userId") Long userId, @Param("genre") String genre);

    /** Géneros distintos presentes en el catálogo disponible, orden alfabético. */
    @Query("select distinct s.genre from Song s where s.available = true "
            + "and s.genre is not null and s.genre <> '' order by s.genre")
    List<String> findDistinctAvailableGenres();

    /** Géneros distintos presentes en el catálogo (ignora nulos y vacíos). */
    @Query("select distinct s.genre from Song s where s.genre is not null and s.genre <> ''")
    List<String> findDistinctGenres();
}
