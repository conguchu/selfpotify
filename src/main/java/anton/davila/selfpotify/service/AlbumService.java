package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.repository.AlbumRepository;
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

    @Transactional
    public Album update(long id, Album albumData) {
        log.info("Actualizando álbum con ID: {}", id);
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el álbum con ID " + id));
        album.copy(albumData);
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
