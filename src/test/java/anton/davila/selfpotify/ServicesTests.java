package anton.davila.selfpotify;

import anton.davila.selfpotify.entity.music.Album;
import anton.davila.selfpotify.entity.music.Song;
import anton.davila.selfpotify.service.SongService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ServicesTests {

    @Autowired
    SongService songService;

    @Test
    void contextLoads() {
    }

    @Test
    void crearCancion() {
        Song s = new Song();
        s.setGenre("Hip-Hop");
        s.setTitle("Hola que tal");
        songService.add(s);

        assert songService.getAll().size() >= 1;
    }

}
