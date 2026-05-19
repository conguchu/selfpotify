package anton.davila.selfpotify.user.listen.repository;

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
}
