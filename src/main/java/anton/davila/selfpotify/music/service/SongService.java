package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.AlbumRepository;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.external.CoverApiService;
import anton.davila.selfpotify.music.service.external.GenreApiService;
import anton.davila.selfpotify.user.listen.repository.UserSongListenRepository;
import anton.davila.selfpotify.music.service.external.LastFmService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
    private UserSongListenRepository userSongListenRepository;

    @Autowired
    private GenreApiService genreApiService;

    @Autowired
    private CoverApiService coverApiService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private LastFmService lastFmService;

    @Autowired
    private AlbumRepository albumRepository;


    // =====================================
    // ----- CRUD
    // =====================================
    public Song add(Song s) {
        log.info("Intentando añadir una nueva canción: {}", s.getTitle());
        Song saved = songRepository.save(s);
        genreApiService.applyGenreIfMissing(s);
        coverApiService.applyCoverIfMissing(s);
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
    /**
     * Reasigna los artistas de una canción a partir de sus ids. Usado por el panel
     * para cambiar el artista de una canción ya existente (modal de búsqueda).
     */
    @Transactional
    public Song setArtists(long id, List<Long> artistIds) {
        Song song = getById(id).orElseThrow(
                () -> new RuntimeException("No se ha encontrado la cancion con ID " + id));
        List<Artist> artists = (artistIds == null) ? List.of()
                : artistIds.stream()
                    .map(artistRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        song.setArtists(artists);
        log.info("Canción {} reasignada a {} artista(s)", id, artists.size());
        return song;
    }

    @Transactional
    public Song delete(long id) {
        log.warn("Intentando eliminar la canción con ID: {}", id);
        Optional<Song> s = getById(id);
        if (s.isEmpty()) {
            log.error("Error al eliminar: No existe la canción con ID {}", id);
            throw new RuntimeException("No se ha encontrado la cancion con ID " + id);
        }
        Song song = s.get();
        // liberamos primero la FK de la tabla cruzada de escuchas
        userSongListenRepository.deleteBySongId(id);
        songRepository.delete(song);
        log.info("Canción con ID {} eliminada correctamente", id);
        return song;
    }

    @Transactional
    public List<Song> saveMany(List<Song> songs) {
        log.warn("Guardando una lista de " + songs.size() + " canciones...");

        songRepository.saveAll(songs);

        // Autocompletado idempotente "si falta": género (Last.fm) y carátulas
        // (embebida → fuentes keyless). Se hace tras persistir para tener ids.
        songs.forEach(genreApiService::applyGenreIfMissing);
        songs.forEach(coverApiService::applyCoverIfMissing);

        log.info("Guardadas " + songs.size() + " canciones correctamente.");
        return songs;
    }

    public List<Song> loadFolder(String folderPath) {
        log.info("Iniciando escaneo de carpeta: {}", folderPath);

        List<Song> songsToSave;

        // Cachés por lote para no duplicar artistas/álbumes ni consultar la BBDD por cada canción.
        Map<String, Artist> artistCache = new HashMap<>();
        Map<String, Album> albumCache = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath))) {
            songsToSave = paths
                    .filter(Files::isRegularFile)
                    .filter(this::isAudioFile)
                    .map(path -> safeExtractMetadata(path, artistCache, albumCache))
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
        Map<String, Album> albumCache = new HashMap<>();

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

                Song song = safeExtractMetadata(path, artistCache, albumCache);
                if (song == null) {
                    failed++;
                    continue;
                }
                song.setSongPath(absolutePath);
                songRepository.save(song);
                genreApiService.applyGenreIfMissing(song);
                coverApiService.applyCoverIfMissing(song);
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

    private Song safeExtractMetadata(Path path, Map<String, Artist> artistCache, Map<String, Album> albumCache) {
        try {
            return extractMetadata(path.toFile(), artistCache, albumCache);
        } catch (Exception e) {
            log.warn("No se pudieron extraer metadatos de {}: {}", path, e.getMessage());
            return null;
        }
    }
    /**
     * Métºdo auxiliar para extraer los metadatos de un archivo físico y mapearlo a la entidad Song.
     */
    private Song extractMetadata(File file, Map<String, Artist> artistCache, Map<String, Album> albumCache) {
        Song song = new Song();
        song.setSongPath(file.getAbsolutePath());

        // Respaldo a partir del nombre de archivo (convención "Artista - Título.ext").
        NameParts fromFileName = parseFileName(file.getName());

        String tagTitle = null;
        String tagArtist = null;
        String tagAlbum = null;

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
                tagAlbum = tag.getFirst(FieldKey.ALBUM);

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
        Artist artist = null;
        String artistName = (tagArtist != null && !tagArtist.isBlank()) ? tagArtist : fromFileName.artist();
        if (artistName != null && !artistName.isBlank()) {
            artist = resolveArtist(artistName.trim(), artistCache);
            song.setArtists(List.of(artist));
        } else {
            log.warn("No se pudo determinar el artista del archivo '{}': no tiene tag ARTIST " +
                    "ni sigue la convención 'Artista - Título'.", file.getName());
        }

        // Álbum: si el tag ID3 ALBUM existe, se resuelve/crea y se enlaza con la
        // canción y su artista (para poder rellenar luego Album.picture_url).
        if (tagAlbum != null && !tagAlbum.isBlank()) {
            song.setAlbum(resolveAlbum(tagAlbum.trim(), artist, albumCache));
        }

        return song;
    }

    /**
     * Resuelve la entidad {@link Artist} de un nombre detectado en los metadatos.
     * <p>
     * El nombre crudo del tag ID3 es inconsistente entre archivos (emojis,
     * espacios sobrantes, alias, mayúsculas...), de modo que el mismo artista
     * acaba en varias filas. Para evitarlo:
     * <ol>
     *   <li>Se limpia el nombre de adornos ({@link #cleanArtistName}).</li>
     *   <li>Se consulta Last.fm para obtener el nombre canónico y el MBID
     *       (identificador estable de MusicBrainz).</li>
     *   <li>Se empareja primero por MBID y, en su defecto, por nombre. Si una
     *       fila existente no tenía MBID, se le rellena (backfill).</li>
     * </ol>
     * Si Last.fm no está configurado o no reconoce al artista, se cae a la
     * búsqueda por nombre limpio, que ya unifica los casos triviales (espacios,
     * mayúsculas). Usa una caché por lote para no repetir consultas.
     */
    private Artist resolveArtist(String rawName, Map<String, Artist> artistCache) {
        String cleaned = cleanArtistName(rawName);
        if (cleaned.isBlank()) {
            cleaned = rawName.trim();
        }
        String cacheKey = cleaned.toLowerCase();
        Artist cached = artistCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Optional<LastFmService.ArtistIdentity> identity = lastFmService.resolveArtist(cleaned);
        Artist artist;

        if (identity.isPresent() && identity.get().mbid() != null) {
            String mbid = identity.get().mbid();
            String canonicalName = identity.get().name();
            artist = artistRepository.findByMbid(mbid)
                    .orElseGet(() -> artistRepository.findByNameIgnoreCase(canonicalName)
                            .map(existing -> backfillMbid(existing, mbid))
                            .orElseGet(() -> createArtist(canonicalName, mbid)));
        } else {
            String name = identity.map(LastFmService.ArtistIdentity::name).orElse(cleaned);
            artist = artistRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> createArtist(name, null));
        }

        artistCache.put(cacheKey, artist);
        // También por nombre canónico: otras variantes del mismo lote que
        // resuelvan a este nombre reusan la entrada sin re-consultar Last.fm.
        artistCache.putIfAbsent(artist.getName().toLowerCase(), artist);
        return artist;
    }

    /** Rellena el MBID de un artista existente que aún no lo tenía. */
    private Artist backfillMbid(Artist artist, String mbid) {
        if (artist.getMbid() == null || artist.getMbid().isBlank()) {
            artist.setMbid(mbid);
            log.info("Asignado MBID '{}' al artista existente '{}' (id={}).",
                    mbid, artist.getName(), artist.getId());
            return artistRepository.save(artist);
        }
        return artist;
    }

    private Artist createArtist(String name, String mbid) {
        Artist artist = new Artist();
        artist.setName(name);
        artist.setMbid(mbid);
        log.info("Creando nuevo artista detectado en el escaneo: '{}' (mbid={}).", name, mbid);
        return artistRepository.save(artist);
    }

    /**
     * Limpia el nombre del artista de adornos que impiden emparejar variantes:
     * emojis, símbolos y espacios redundantes. Conserva letras, dígitos,
     * espacios y la puntuación habitual en nombres ('&', '-', '.', '\'', '()').
     */
    private String cleanArtistName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || "&-.'()/".indexOf(c) >= 0) {
                sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Busca un álbum por nombre (sin distinguir mayúsculas); si no existe, lo crea
     * vinculándolo con el artista de la canción. Usa una caché por lote para no
     * duplicar el mismo álbum entre las pistas de un mismo escaneo. Disponer del
     * álbum durante el escaneo permite que {@code CoverApiService} pueble
     * {@code Album.picture_url}.
     */
    private Album resolveAlbum(String name, Artist artist, Map<String, Album> albumCache) {
        return albumCache.computeIfAbsent(name.toLowerCase(), key ->
                albumRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> {
                            Album album = new Album();
                            album.setName(name);
                            if (artist != null) {
                                album.setArtists(List.of(artist));
                            }
                            log.info("Creando nuevo álbum detectado en el escaneo: {}", name);
                            return albumRepository.save(album);
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
     * Devuelve las 10 canciones más escuchadas de un género, ordenadas por su
     * recuento de escuchas derivado de la tabla {@code user_song_listen}.
     * @param genre nombre del género
     * @return lista (máx. 10) de canciones del género ordenadas por escuchas desc
     */
    public List<Song> getTop10ByGenre(String genre) {
        log.info("Recuperando las 10 canciones más escuchadas del género: {}", genre);
        return userSongListenRepository.findSongsByGenreOrderByGlobalListensDesc(genre, PageRequest.of(0, 10));
    }

    /**
     * Recuento global de escuchas por canción, derivado de los eventos de
     * {@code user_song_listen}. Devuelve un mapa id→escuchas que incluye sólo
     * las canciones con al menos una escucha; el resto se asume 0 en quien lo
     * consuma. Una única consulta agrupada evita el N+1 al exponer la
     * popularidad en los listados.
     *
     * @return mapa de id de canción a número de escuchas
     */
    public Map<Long, Long> getListenCountsBySong() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : userSongListenRepository.countListensGroupedBySong()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    /**
     * Recuento global de escuchas de una única canción, derivado de los eventos.
     *
     * @param songId id de la canción
     * @return número de escuchas registradas de esa canción
     */
    public long getListenCount(long songId) {
        return userSongListenRepository.countBySong_Id(songId);
    }

}


