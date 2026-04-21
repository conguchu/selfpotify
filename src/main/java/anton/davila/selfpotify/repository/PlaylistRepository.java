package anton.davila.selfpotify.repository;

import anton.davila.selfpotify.entity.music.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
}
