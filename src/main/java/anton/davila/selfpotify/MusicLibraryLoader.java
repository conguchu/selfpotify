package anton.davila.selfpotify;

import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.SongService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
@Order(2)
public class MusicLibraryLoader implements CommandLineRunner {

    @Autowired
    private SongService songService;

    @Autowired
    private SongRepository songRepository;

    @Value("${app.music.import-folder:#{systemProperties['user.home'] + '/Downloads'}}")
    private String importFolder;

    @Override
    public void run(String... args) {
        if (songRepository.count() > 0) {
            log.info("Ya hay canciones en la BD ({}), se omite la importación inicial", songRepository.count());
            return;
        }

        Path folder = Paths.get(importFolder);
        if (!Files.isDirectory(folder)) {
            log.warn("La carpeta de importación no existe: {}", importFolder);
            return;
        }

        log.info("Importando canciones desde {}", importFolder);
        var loaded = songService.loadFolder(importFolder);
        log.info("Importación inicial completada: {} canciones", loaded.size());
    }
}
