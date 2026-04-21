package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Artist;
import anton.davila.selfpotify.repository.ArtistRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ArtistService {

    @Autowired
    private ArtistRepository artistRepository;

    public Artist add(Artist a) {
        log.info("Añadiendo nuevo artista: {}", a.getName());
        return artistRepository.save(a);
    }

    public List<Artist> getAll() {
        log.info("Recuperando todos los artistas");
        return artistRepository.findAll();
    }

    public Optional<Artist> getById(long id) {
        log.info("Buscando artista por ID: {}", id);
        return artistRepository.findById(id);
    }

    @Transactional
    public Artist update(long id, Artist artistData) {
        log.info("Actualizando artista con ID: {}", id);
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el artista con ID " + id));
        artist.copy(artistData);
        return artist;
    }

    public Artist delete(long id) {
        log.warn("Eliminando artista con ID: {}", id);
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el artista con ID " + id));
        artistRepository.delete(artist);
        return artist;
    }
}
