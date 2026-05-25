package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findFirstBySongPath(String songPath);
}
