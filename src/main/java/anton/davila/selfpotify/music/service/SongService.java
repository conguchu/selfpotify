package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private LastFmService lastFmService;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        log.info("Intentando añadir una nueva canción: {}", s.getTitle());
        Song saved = songRepository.save(s);
        applyGenreIfMissing(s);
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

    @Transactional
    public List<Song> saveMany(List<Song> songs) {
        log.warn("Guardando una lista de " + songs.size() + " canciones...");

        songs.forEach(this::applyGenreIfMissing);

        songRepository.saveAll(songs);
        log.info("Guardadas " + songs.size() + " canciones correctamente.");
        return songs;
    }

    public List<Song> loadFolder(String folderPath) {
        log.info("Iniciando escaneo de carpeta: {}", folderPath);

        List<Song> songsToSave;

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            songsToSave = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .map(this::safeExtractMetadata)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (IOException e) {
            log.error("Error al leer el directorio: {}", folderPath, e);
            throw new RuntimeException("No se pudo acceder a la ruta", e);
        }

        log.info("Escaneo finalizado. Se prepararon {} canciones.", songsToSave.size());

        // Llamamos al métºdo transaccional para la persistencia final
        return saveMany(songsToSave);
    }

    /**
     * Comprueba que está disponible el archivo de música asociado con una cancion
     * @param song
     * @return
     */
    public boolean isPathAvailable(Song song) {

        String songPath = song.getSongPath();

        if (songPath == null || songPath.isBlank()) {
            log.warn("La ruta proporcionada está vacía o es nula");
            return false;
        }
        Path path = Paths.get(songPath);
        boolean available = Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path);
        if (!available) {
            log.warn("El archivo no está disponible: {}", songPath);
            song.setAvailable(false);
            songRepository.save(song);
        }
        return available;
    }

    private boolean isAudioFile(Path path) {
        String fileName = path.toString().toLowerCase();
        return fileName.endsWith(".mp3") || fileName.endsWith(".wav");
    }

    private Song safeExtractMetadata(Path path) {
        try {
            return extractMetadata(path.toFile());
        } catch (Exception e) {
            log.warn("No se pudieron extraer metadatos de {}: {}", path, e.getMessage());
            return null;
        }
    }
    /**
     * Métºdo auxiliar para extraer los metadatos de un archivo físico y mapearlo a la entidad Song.
     */
    private Song extractMetadata(File file) {
        Song song = new Song();
        song.setSongPath(file.getAbsolutePath());
        song.setListeners(0); // Valor por defecto para canciones nuevas

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            // Extracción de datos del Header (información técnica del audio)
            if (header != null) {
                // getTrackLength() devuelve segundos. Lo pasamos a ms según tu entidad.
                song.setDuration_ms(header.getTrackLength() * 1000);
            }

            // extracción de datos del Tag (metadatos ID3)
            if (tag != null) {
                // titulo
                // si no hay metadato, usamos el nombre del archivo como fallback
                String title = tag.getFirst(FieldKey.TITLE);
                song.setTitle((title != null && !title.isBlank()) ? title : file.getName());

                // género
                song.setGenre(tag.getFirst(FieldKey.GENRE));

                // BPM
                String bpmStr = tag.getFirst(FieldKey.BPM);
                if (bpmStr != null && !bpmStr.isBlank()) {
                    try {
                        song.setBpm(Integer.parseInt(bpmStr));
                    } catch (NumberFormatException e) {
                        log.debug("El formato BPM del archivo {} no es un número válido: {}", file.getName(), bpmStr);
                    }
                }
            } else {
                // si no hay tags en absoluto, aseguramos que al menos tenga un título
                song.setTitle(file.getName());
            }

        } catch (Exception e) {
            // capturamos cualquier excepción de JAudioTagger para que un archivo corrupto no detenga el lote completo
            log.warn("No se pudieron extraer todos los metadatos del archivo: {}. Usando datos básicos.", file.getName());
            song.setTitle(file.getName());
        }

        return song;
    }

    /**
     * Incrementa una escucha a una canción
     * @param id id de la cancion
     */
    @Transactional
    public void incrementListeners(long id) {
        songRepository.incrementListeners(id);
    }

    /**
     * Si la canción no tiene género asignado, lo consulta en Last.fm
     * a partir de su título y primer artista, y lo persiste.
     * Si ya tiene género, no hace nada.
     *
     * @param song canción a clasificar (debe estar gestionada o tener id)
     * @return la canción (actualizada si se le ha aplicado género)
     */
    @Transactional
    public Song applyGenreIfMissing(Song song) {
        if (song == null) {
            log.warn("applyGenreIfMissing: canción nula, se ignora.");
            return null;
        }
        if (song.getGenre() != null && !song.getGenre().isBlank()) {
            log.debug("La canción '{}' ya tiene género '{}', se omite.", song.getTitle(), song.getGenre());
            return song;
        }

        String artistName = primaryArtistName(song);
        if (artistName == null) {
            log.warn("No se puede clasificar la canción '{}' (id={}): no tiene artista asociado.",
                    song.getTitle(), song.getId());
            return song;
        }

        Optional<String> genre = lastFmService.fetchGenre(artistName, song.getTitle());
        if (genre.isEmpty()) {
            log.info("Last.fm no devolvió género para '{} - {}'.", artistName, song.getTitle());
            return song;
        }

        song.setGenre(genre.get());
        Song saved = songRepository.save(song);
        log.info("Género '{}' asignado a la canción '{}' (id={}).",
                saved.getGenre(), saved.getTitle(), saved.getId());
        return saved;
    }

    private String primaryArtistName(Song song) {
        List<Artist> artists = song.getArtists();
        if (artists == null || artists.isEmpty()) return null;
        Artist first = artists.get(0);
        if (first == null || first.getName() == null || first.getName().isBlank()) return null;
        return first.getName();
    }

}


