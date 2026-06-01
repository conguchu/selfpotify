package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    public Album add(Album a) {
        log.info("Añadiendo nuevo álbum: {}", a.getName());
        return albumRepository.save(a);
    }

    public List<Album> getAll() {
        log.info("Recuperando todos los álbumes");
        return albumRepository.findAll();
    }

    public Optional<Album> getById(long id) {
        log.info("Buscando álbum por ID: {}", id);
        return albumRepository.findById(id);
    }

    /**
     * Edición manual de un álbum desde el panel: solo nombre y portada. No usa
     * {@link Album#copy(Album)} a propósito, porque ese método sobrescribe también
     * las asociaciones (artistas, canciones) y un body parcial las dejaría a null,
     * borrando el vínculo {@code album_artist}.
     */
    @Transactional
    public Album updateMeta(long id, String name, String photoUrl) {
        log.info("Actualizando álbum con ID: {}", id);
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el álbum con ID " + id));
        if (name != null && !name.isBlank()) {
            album.setName(name.trim());
        }
        album.setPicture_url(photoUrl);
        return album;
    }

    public Album delete(long id) {
        log.warn("Eliminando álbum con ID: {}", id);
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el álbum con ID " + id));
        albumRepository.delete(album);
        return album;
    }
}
