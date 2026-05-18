package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Modifying
    @Query("update Song s set s.listeners = s.listeners + 1 where s.id = :id")
    void incrementListeners(@Param("id") Long id);

    @Query("select s from Song s join s.artists a where a.id = :artistId order by s.listeners desc limit 10")
    List<Song> findTop10ByArtistIdOrderByListenersDesc(@Param("artistId") Long artistId);

    Optional<Song> findFirstBySongPath(String songPath);
}
