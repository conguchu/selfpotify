package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.PlaylistRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    public Playlist add(Playlist p) {
        log.info("Añadiendo nueva playlist");
        return playlistRepository.save(p);
    }

    public List<Playlist> getAll() {
        log.info("Recuperando todas las playlists");
        return playlistRepository.findAll();
    }

    public List<Playlist> getByUser(anton.davila.selfpotify.user.entity.User user) {
        log.info("Recuperando playlists del usuario: {}", user.getUsername());
        return playlistRepository.findByCreator(user);
    }

    public List<Playlist> getPublicByUser(anton.davila.selfpotify.user.entity.User user) {
        log.info("Recuperando playlists públicas del usuario: {}", user.getUsername());
        return playlistRepository.findByCreatorAndIsPublicTrue(user);
    }

    public List<Playlist> getAllPublic() {
        log.info("Recuperando todas las playlists públicas");
        return playlistRepository.findByIsPublicTrue();
    }

    public Optional<Playlist> getById(long id) {
        log.info("Buscando playlist por ID: {}", id);
        return playlistRepository.findById(id);
    }

    @Transactional
    public Playlist update(long id, Playlist playlistData) {
        log.info("Actualizando playlist con ID: {}", id);
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la playlist con ID " + id));
        playlist.copy(playlistData);
        return playlist;
    }

    public Playlist delete(long id) {
        log.warn("Eliminando playlist con ID: {}", id);
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la playlist con ID " + id));
        playlistRepository.delete(playlist);
        return playlist;
    }

    /** Añade una canción a la playlist (sin duplicar) y recalcula la duración total. */
    @Transactional
    public Playlist addSong(long playlistId, Song song) {
        log.info("Añadiendo canción {} a la playlist {}", song.getId(), playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("No se encontró la playlist con ID " + playlistId));
        if (playlist.getSongs() == null) {
            playlist.setSongs(new ArrayList<>());
        }
        boolean alreadyPresent = playlist.getSongs().stream()
                .anyMatch(s -> s.getId().equals(song.getId()));
        if (!alreadyPresent) {
            playlist.getSongs().add(song);
            recalcDuration(playlist);
        }
        return playlist;
    }

    /** Quita una canción de la playlist y recalcula la duración total. */
    @Transactional
    public Playlist removeSong(long playlistId, Song song) {
        log.info("Quitando canción {} de la playlist {}", song.getId(), playlistId);
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("No se encontró la playlist con ID " + playlistId));
        if (playlist.getSongs() != null
                && playlist.getSongs().removeIf(s -> s.getId().equals(song.getId()))) {
            recalcDuration(playlist);
        }
        return playlist;
    }

    private void recalcDuration(Playlist playlist) {
        int total = 0;
        if (playlist.getSongs() != null) {
            for (Song s : playlist.getSongs()) {
                total += s.getDuration_ms();
            }
        }
        playlist.setDuration_ms(total);
    }
}
