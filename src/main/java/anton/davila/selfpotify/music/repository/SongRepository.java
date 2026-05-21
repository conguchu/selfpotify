package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Modifying
    @Query("update Song s set s.listeners = s.listeners + 1 where s.id = :id")
    void incrementListeners(@Param("id") Long id);

    Optional<Song> findFirstBySongPath(String songPath);
}
