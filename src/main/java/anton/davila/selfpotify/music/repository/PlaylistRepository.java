package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByCreator(User creator);
    List<Playlist> findByCreatorAndIsPublicTrue(User creator);
    List<Playlist> findByIsPublicTrue();
}
