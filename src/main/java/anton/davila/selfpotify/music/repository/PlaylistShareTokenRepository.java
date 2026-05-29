package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.PlaylistShareToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaylistShareTokenRepository extends JpaRepository<PlaylistShareToken, Long> {

    Optional<PlaylistShareToken> findByToken(String token);

    void deleteByPlaylist(Playlist playlist);
}
