package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
}
