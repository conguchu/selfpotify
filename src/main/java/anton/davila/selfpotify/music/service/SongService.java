package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.external.GenreApiService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class SongService {

    @Autowired
    private SongRepository songRepository;

    @Autowired
    private GenreApiService genreApiService;

    @Autowired
    private ArtistRepository artistRepository;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        log.info("Intentando añadir una nueva canción: {}", s.getTitle());
        Song saved = songRepository.save(s);
        genreApiService.applyGenreIfMissing(s);
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

        songs.forEach(genreApiService::applyGenreIfMissing);

        songRepository.saveAll(songs);
        log.info("Guardadas " + songs.size() + " canciones correctamente.");
        return songs;
    }

    public List<Song> loadFolder(String folderPath) {
        log.info("Iniciando escaneo de carpeta: {}", folderPath);

        List<Song> songsToSave;

        // Cache de artistas para no duplicarlos ni consultar la BBDD por cada canción del lote.
        Map<String, Artist> artistCache = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            songsToSave = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .map(path -> safeExtractMetadata(path, artistCache))
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
     * Re-escaneo idempotente: por cada audio de la carpeta inserta si su songPath
     * no estaba en BBDD, recupera (available=true) si estaba marcado como no
     * disponible, y deja intactas las canciones ya presentes y disponibles.
     */
    public RescanStats rescanFolder(String folderPath) {
        log.info("Iniciando re-escaneo (idempotente) de carpeta: {}", folderPath);

        int added = 0, recovered = 0, skipped = 0, failed = 0;
        Map<String, Artist> artistCache = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            List<Path> audioPaths = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .toList();

            for (Path path : audioPaths) {
                String absolutePath = path.toAbsolutePath().normalize().toString();
                Optional<Song> existing = songRepository.findFirstBySongPath(absolutePath);
                if (existing.isPresent()) {
                    Song s = existing.get();
                    if (!s.isAvailable()) {
                        s.setAvailable(true);
                        songRepository.save(s);
                        recovered++;
                    } else {
                        skipped++;
                    }
                    continue;
                }

                Song song = safeExtractMetadata(path, artistCache);
                if (song == null) {
                    failed++;
                    continue;
                }
                song.setSongPath(absolutePath);
                songRepository.save(song);
                genreApiService.applyGenreIfMissing(song);
                added++;
            }
        } catch (IOException e) {
            log.error("Error al releer la carpeta: {}", folderPath, e);
            throw new RuntimeException("No se pudo acceder a la ruta", e);
        }

        log.info("Re-escaneo de {} terminado: added={}, recovered={}, skipped={}, failed={}",
                folderPath, added, recovered, skipped, failed);
        return new RescanStats(added, recovered, skipped, failed);
    }

    public record RescanStats(int added, int recovered, int skipped, int failed) {
        public RescanStats plus(RescanStats other) {
            return new RescanStats(
                    added + other.added,
                    recovered + other.recovered,
                    skipped + other.skipped,
                    failed + other.failed
            );
        }
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

    private Song safeExtractMetadata(Path path, Map<String, Artist> artistCache) {
        try {
            return extractMetadata(path.toFile(), artistCache);
        } catch (Exception e) {
            log.warn("No se pudieron extraer metadatos de {}: {}", path, e.getMessage());
            return null;
        }
    }
    /**
     * Métºdo auxiliar para extraer los metadatos de un archivo físico y mapearlo a la entidad Song.
     */
    private Song extractMetadata(File file, Map<String, Artist> artistCache) {
        Song song = new Song();
        song.setSongPath(file.getAbsolutePath());
        song.setListeners(0); // Valor por defecto para canciones nuevas

        // Respaldo a partir del nombre de archivo (convención "Artista - Título.ext").
        NameParts fromFileName = parseFileName(file.getName());

        String tagTitle = null;
        String tagArtist = null;

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
                tagTitle = tag.getFirst(FieldKey.TITLE);
                tagArtist = tag.getFirst(FieldKey.ARTIST);

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
            }

        } catch (Exception e) {
            // capturamos cualquier excepción de JAudioTagger para que un archivo corrupto no detenga el lote completo
            log.warn("No se pudieron extraer todos los metadatos del archivo: {}. Usando datos básicos.", file.getName());
        }

        // Título: tag ID3 si existe; si no, lo deducido del nombre de archivo.
        song.setTitle((tagTitle != null && !tagTitle.isBlank()) ? tagTitle : fromFileName.title());

        // Artista: tag ID3 si existe; si no, el deducido del nombre de archivo.
        String artistName = (tagArtist != null && !tagArtist.isBlank()) ? tagArtist : fromFileName.artist();
        if (artistName != null && !artistName.isBlank()) {
            song.setArtists(List.of(resolveArtist(artistName.trim(), artistCache)));
        } else {
            log.warn("No se pudo determinar el artista del archivo '{}': no tiene tag ARTIST " +
                    "ni sigue la convención 'Artista - Título'.", file.getName());
        }

        return song;
    }

    /**
     * Busca un artista por nombre (sin distinguir mayúsculas); si no existe, lo crea.
     * Usa una caché por lote para evitar duplicados dentro del mismo escaneo.
     */
    private Artist resolveArtist(String name, Map<String, Artist> artistCache) {
        return artistCache.computeIfAbsent(name.toLowerCase(), key ->
                artistRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Artist artist = new Artist();
                            artist.setName(name);
                            log.info("Creando nuevo artista detectado en el escaneo: {}", name);
                            return artistRepository.save(artist);
                        }));
    }

    /**
     * Deduce artista y título a partir del nombre de archivo siguiendo la
     * convención "Artista - Título.ext". Si no hay separador " - ", el artista
     * queda nulo y el título es el nombre del archivo sin extensión.
     */
    private NameParts parseFileName(String fileName) {
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        int sep = base.indexOf(" - ");
        if (sep > 0) {
            String artist = base.substring(0, sep).trim();
            String title = base.substring(sep + 3).trim();
            return new NameParts(artist.isBlank() ? null : artist,
                    title.isBlank() ? base.trim() : title);
        }
        return new NameParts(null, base.trim());
    }

    /** Artista y título deducidos del nombre de archivo. */
    private record NameParts(String artist, String title) {}

    /**
     * Incrementa una escucha a una canción
     * @param id id de la cancion
     */
    @Transactional
    public void incrementListeners(long id) {
        songRepository.incrementListeners(id);
    }

}


