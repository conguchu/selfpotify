package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.PlaylistCollaborator;
import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistCollaboratorRepository extends JpaRepository<PlaylistCollaborator, Long> {

    Optional<PlaylistCollaborator> findByPlaylistAndUser(Playlist playlist, User user);

    boolean existsByPlaylistAndUser(Playlist playlist, User user);

    List<PlaylistCollaborator> findByPlaylist(Playlist playlist);

    List<PlaylistCollaborator> findByUser(User user);

    void deleteByPlaylist(Playlist playlist);
}
