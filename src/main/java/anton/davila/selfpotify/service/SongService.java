package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        return songRepository.save(s);
    }
    public List<Song> getAll() {
        return songRepository.findAll();
    }
    public Optional<Song> getById(long id) {
        return songRepository.findById(id);
    }
    public Song update(long id, Song song) {
        Optional<Song> old = getById(id);
        if (old.isEmpty()) {
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        return null;
        // todo: song.copy(old)
        // (metodo para copiar los atributos del objeto sin tener que hacerlo a mano siempre)
    }




}
