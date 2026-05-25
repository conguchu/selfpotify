package anton.davila.selfpotify.user.listen.repository;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.user.listen.entity.UserSongListen;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSongListenRepository extends JpaRepository<UserSongListen, Long> {

    /** Número de escuchas almacenadas para un usuario. */
    long countByUser_Id(Long userId);

    /** Escuchas de un usuario ordenadas de la más antigua a la más reciente. */
    List<UserSongListen> findByUser_IdOrderByListenedAtAsc(Long userId, Pageable pageable);

    /** Borra todas las escuchas que apuntan a una canción (libera la FK al borrarla). */
    @Modifying
    @Query("delete from UserSongListen e where e.song.id = :songId")
    void deleteBySongId(@Param("songId") Long songId);

    // =====================================================================
    // ----- Conteos derivados (popularidad GLOBAL) a partir de los eventos
    // -----  Una escucha de canción implica escucha de su álbum, de cada uno
    // -----  de sus artistas y de su género. Se cuenta sobre user_song_listen
    // -----  en vez de mantener contadores numéricos en las entidades.
    // =====================================================================

    /** Total global de escuchas registradas de una canción. */
    long countBySong_Id(Long songId);

    /**
     * Conteo global de escuchas agrupado por canción, para todas las canciones
     * que tengan al menos una escucha. Cada fila es {@code [songId, total]}.
     * Permite construir un mapa id→escuchas de una sola consulta y evitar el
     * N+1 al exponer la popularidad en los listados (p. ej. {@code GET /api/songs}).
     */
    @Query("select e.song.id, count(e) from UserSongListen e group by e.song.id")
    List<Object[]> countListensGroupedBySong();

    /** Total global de escuchas de todas las canciones de un álbum. */
    @Query("select count(e) from UserSongListen e where e.song.album.id = :albumId")
    long countByAlbumId(@Param("albumId") Long albumId);

    /** Total global de escuchas de todas las canciones en las que participa un artista. */
    @Query("select count(e) from UserSongListen e join e.song s join s.artists a where a.id = :artistId")
    long countByArtistId(@Param("artistId") Long artistId);

    /** Total global de escuchas de todas las canciones de un género. */
    @Query("select count(e) from UserSongListen e where e.song.genre = :genre")
    long countByGenre(@Param("genre") String genre);

    /**
     * Artistas más escuchados globalmente, ordenados por número de escuchas
     * derivado de los eventos. Sustituye al antiguo contador
     * {@code Artist.listeners}. Usar {@code Pageable} para acotar el top-N.
     */
    @Query("select a from UserSongListen e join e.song s join s.artists a "
            + "group by a order by count(e) desc")
    List<Artist> findArtistsByGlobalListensDesc(Pageable pageable);

    /**
     * Canciones de un género ordenadas por escuchas globales (desc). Sólo
     * incluye canciones disponibles. Sustituye a
     * {@code findTop10ByGenreOrderByListenersDesc}.
     */
    @Query("select s from UserSongListen e join e.song s "
            + "where s.genre = :genre and s.available = true "
            + "group by s order by count(e) desc")
    List<Song> findSongsByGenreOrderByGlobalListensDesc(@Param("genre") String genre, Pageable pageable);

    /**
     * Canciones de un artista ordenadas por escuchas globales (desc). Sustituye
     * a {@code findTop10ByArtistIdOrderByListenersDesc}.
     */
    @Query("select s from UserSongListen e join e.song s join s.artists a "
            + "where a.id = :artistId "
            + "group by s order by count(e) desc")
    List<Song> findSongsByArtistOrderByGlobalListensDesc(@Param("artistId") Long artistId, Pageable pageable);

    // =====================================================================
    // ----- Conteos derivados POR USUARIO (para personalizar el feed)
    // =====================================================================

    /**
     * Artistas más escuchados por un usuario concreto, ordenados por número de
     * escuchas propias (desc). Base de la recomendación personalizada.
     */
    @Query("select a from UserSongListen e join e.song s join s.artists a "
            + "where e.user.id = :userId "
            + "group by a order by count(e) desc")
    List<Artist> findTopArtistsByUserListens(@Param("userId") Long userId, Pageable pageable);

    /**
     * Géneros más escuchados por un usuario concreto, ordenados por número de
     * escuchas propias (desc). Se ignoran las canciones sin género.
     */
    @Query("select s.genre from UserSongListen e join e.song s "
            + "where e.user.id = :userId and s.genre is not null and s.genre <> '' "
            + "group by s.genre order by count(e) desc")
    List<String> findTopGenresByUserListens(@Param("userId") Long userId, Pageable pageable);
}
