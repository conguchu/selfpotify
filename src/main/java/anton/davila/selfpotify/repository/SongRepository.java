package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.music.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
}
