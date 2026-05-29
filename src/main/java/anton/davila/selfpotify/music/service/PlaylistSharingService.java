package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.PlaylistCollaborator;
import anton.davila.selfpotify.music.entity.PlaylistShareToken;
import anton.davila.selfpotify.music.repository.PlaylistCollaboratorRepository;
import anton.davila.selfpotify.music.repository.PlaylistShareTokenRepository;
import anton.davila.selfpotify.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * Gestiona la colaboración en playlists: generación y canje de magic links
 * ({@link PlaylistShareToken}) y la relación de colaboradores
 * ({@link PlaylistCollaborator}). {@link PlaylistService} sigue centrado en el
 * CRUD de la playlist en sí.
 */
@Slf4j
@Service
public class PlaylistSharingService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @Autowired
    private PlaylistShareTokenRepository shareTokenRepository;

    @Autowired
    private PlaylistCollaboratorRepository collaboratorRepository;

    /** Genera un magic link de un solo uso para la playlist dada y devuelve su token. */
    public String createShareToken(Playlist playlist) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);

        PlaylistShareToken shareToken = new PlaylistShareToken();
        shareToken.setToken(token);
        shareToken.setPlaylist(playlist);
        shareTokenRepository.save(shareToken);

        log.info("Generado magic link para playlist {}", playlist.getId());
        return token;
    }

    /**
     * Canjea un magic link: añade a {@code user} como colaborador de la playlist
     * asociada y consume (elimina) el token. Idempotente respecto al colaborador
     * (no duplica), pero el token siempre se consume.
     *
     * @throws ResponseStatusException 404 si el token no existe o ya fue usado;
     *                                 409 si quien canjea es el propietario.
     */
    @Transactional
    public Playlist redeemToken(String token, User user) {
        PlaylistShareToken shareToken = shareTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Enlace inválido o ya utilizado"));

        Playlist playlist = shareToken.getPlaylist();

        if (playlist.getCreator() != null && playlist.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Eres el propietario de esta playlist");
        }

        if (!collaboratorRepository.existsByPlaylistAndUser(playlist, user)) {
            PlaylistCollaborator collaborator = new PlaylistCollaborator();
            collaborator.setPlaylist(playlist);
            collaborator.setUser(user);
            collaboratorRepository.save(collaborator);
            log.info("Usuario {} se une como colaborador de la playlist {}", user.getId(), playlist.getId());
        }

        shareTokenRepository.delete(shareToken);
        return playlist;
    }

    public boolean isCollaborator(Playlist playlist, User user) {
        return collaboratorRepository.existsByPlaylistAndUser(playlist, user);
    }

    public List<User> collaboratorsOf(Playlist playlist) {
        return collaboratorRepository.findByPlaylist(playlist).stream()
                .map(PlaylistCollaborator::getUser)
                .toList();
    }

    /** Playlists en las que {@code user} figura como colaborador (no creador). */
    public List<Playlist> sharedWith(User user) {
        return collaboratorRepository.findByUser(user).stream()
                .map(PlaylistCollaborator::getPlaylist)
                .toList();
    }

    /** Limpia colaboradores y tokens de una playlist (al borrarla). */
    @Transactional
    public void deleteSharingData(Playlist playlist) {
        collaboratorRepository.deleteByPlaylist(playlist);
        shareTokenRepository.deleteByPlaylist(playlist);
    }
}
