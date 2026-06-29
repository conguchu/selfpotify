package anton.davila.selfpotify.music.repository;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.PlaylistCollaborator;
import anton.davila.selfpotify.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaylistCollaboratorRepository extends JpaRepository<PlaylistCollaborator, Long> {

    Optional<PlaylistCollaborator> findByPlaylistAndUser(Playlist playlist, User user);

    boolean existsByPlaylistAndUser(Playlist playlist, User user);

    List<PlaylistCollaborator> findByPlaylist(Playlist playlist);

    List<PlaylistCollaborator> findByUser(User user);

    /**
     * Colaboradores de varias playlists en una sola consulta, con el {@code user}
     * ya resuelto ({@link EntityGraph}). Permite poblar {@code collaboratorIds} en
     * los listados (/my, /shared) sin incurrir en el N+1 de consultar por playlist.
     */
    @EntityGraph(attributePaths = {"user"})
    List<PlaylistCollaborator> findByPlaylist_IdIn(Collection<Long> playlistIds);

    void deleteByPlaylist(Playlist playlist);
}
