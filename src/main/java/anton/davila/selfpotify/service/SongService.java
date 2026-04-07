package anton.davila.selfpotify.service;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;


    // ----- CRUD
    public Song add(Song s) {
        return songRepository.save(s);
    }

    public List<Song> getAll() {
        return songRepository.findAll();
    }


}
