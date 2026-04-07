package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        log.info("Intentando añadir una nueva canción: {}", s.getTitle());
        Song saved = songRepository.save(s);
        log.debug("Canción guardada con éxito. ID: {}", saved.getId());
        return saved;
    }
    public List<Song> getAll() {
        log.info("Recuperando todas las canciones de la base de datos");
        List<Song> songs =  songRepository.findAll();
        log.debug("Se han encontrado {} canciones", songs.size());
        return songs;
    }
    public Optional<Song> getById(long id) {
        log.info("Buscando canción por ID: {}", id);
        return songRepository.findById(id);
    }
    @Transactional
    public Song update(long id, Song songData) {
        log.info("Iniciando actualización de la canción con ID: {}", id);
        Optional<Song> s = getById(id);
        if (s.isEmpty()) {
            log.error("Error al actualizar: No se encontró la canción con ID {}", id);
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        Song song = s.get();
        song.copy(songData);
        log.info("Canción con ID {} actualizada correctamente en el contexto de persistencia", id);
        return song;

        // (metodo para copiar los atributos del objeto sin tener que hacerlo a mano siempre)
    }
    public Song delete(long id) {
        log.warn("Intentando eliminar la canción con ID: {}", id);
        Optional<Song> s = getById(id);
        if (s.isEmpty()) {
            log.error("Error al eliminar: No existe la canción con ID {}", id);
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        Song song = s.get();
        songRepository.delete(song);
        log.info("Canción con ID {} eliminada correctamente", id);
        return song;
    }


    public List<Song> saveMany(List<Song> songs) {
        log.warn("Guardando una lista de " + songs.size() + " canciones...");
        songRepository.saveAll(songs);
        log.info("Guardadas " + songs.size() + " canciones correctamente.");
        return songs;
    }

}
