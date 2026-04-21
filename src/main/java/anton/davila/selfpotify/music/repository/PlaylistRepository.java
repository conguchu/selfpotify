package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
}
