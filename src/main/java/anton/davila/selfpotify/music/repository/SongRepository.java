package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findFirstBySongPath(String songPath);

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
