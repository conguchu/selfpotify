package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.config.MusicLibraryResolver;
import anton.davila.selfpotify.controllers.dto.SongCommitRequest;
import anton.davila.selfpotify.controllers.dto.SongDraftDTO;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.ArtistRepository;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.external.EmbeddedCoverExtractor;
import lombok.extern.slf4j.Slf4j;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Subida de audios desde el panel en DOS fases, para que el admin pueda revisar y
 * ajustar los metadatos antes de incorporar las canciones a la biblioteca:
 *
 * <ol>
 *   <li><b>Staging</b> ({@link #uploadToStaging}): el audio se guarda en una
 *       carpeta temporal {@code selfpotify_staging/<token>} que <b>no</b> está en
 *       las rutas de escaneo, de modo que el escaneo periódico no lo importe a
 *       medias. Se extraen sus metadatos (título, artista, género, BPM, duración,
 *       carátula embebida) y se devuelven como {@link SongDraftDTO} editables.</li>
 *   <li><b>Commit</b> ({@link #commit}): con los metadatos ya ajustados, el audio
 *       se mueve a {@code selfpotify_added} (volumen de datos en Docker —porque el
 *       de música es read-only— o ruta elegida en local) y se persiste la canción
 *       con su artista resuelto. La carpeta se registra como ruta de escaneo.</li>
 * </ol>
 */
@Slf4j
@Service
public class SongUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp3", ".wav");

    private final ConfigService configService;
    private final MusicLibraryResolver musicLibraryResolver;
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final EmbeddedCoverExtractor coverExtractor;
    private final AppProperties appProperties;

    public SongUploadService(ConfigService configService,
                             MusicLibraryResolver musicLibraryResolver,
                             SongRepository songRepository,
                             ArtistRepository artistRepository,
                             EmbeddedCoverExtractor coverExtractor,
                             AppProperties appProperties) {
        this.configService = configService;
        this.musicLibraryResolver = musicLibraryResolver;
        this.songRepository = songRepository;
        this.artistRepository = artistRepository;
        this.coverExtractor = coverExtractor;
        this.appProperties = appProperties;
    }

    // =====================================================================
    // Fase 1 — staging
    // =====================================================================

    /** Guarda los audios en staging y devuelve sus borradores editables. */
    public List<SongDraftDTO> uploadToStaging(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha subido ningún archivo");
        }
        long maxBytes = appProperties.getUpload().getMaxFileSize().toBytes();
        List<SongDraftDTO> drafts = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            if (!isAllowed(original)) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Formato no soportado: " + original + ". Admitidos: .mp3, .wav");
            }
            if (file.getSize() > maxBytes) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                        "El archivo " + original + " excede el máximo de "
                                + appProperties.getUpload().getMaxFileSize().toMegabytes() + " MB");
            }
            String token = UUID.randomUUID().toString();
            Path dir = stagingDir().resolve(token);
            String fileName = sanitizeFilename(original);
            try {
                Files.createDirectories(dir);
                Files.copy(file.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("No se pudo guardar el audio en staging {}", dir, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se pudo guardar el archivo " + original);
            }
            drafts.add(buildDraft(token, fileName, dir.resolve(fileName)));
        }
        if (drafts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ningún archivo válido para subir");
        }
        return drafts;
    }

    private SongDraftDTO buildDraft(String token, String fileName, Path audioPath) {
        SongDraftDTO d = new SongDraftDTO();
        d.setStagingToken(token);
        d.setFileName(fileName);
        // Valores por defecto a partir del nombre de archivo (convención "Artista - Título").
        NameParts fromName = parseFileName(fileName);
        d.setTitle(fromName.title());
        d.setArtistName(fromName.artist());
        try {
            AudioFile audioFile = AudioFileIO.read(audioPath.toFile());
            AudioHeader header = audioFile.getAudioHeader();
            if (header != null) {
                d.setDuration_ms(header.getTrackLength() * 1000);
            }
            Tag tag = audioFile.getTag();
            if (tag != null) {
                String t = tag.getFirst(FieldKey.TITLE);
                if (t != null && !t.isBlank()) d.setTitle(t);
                String a = tag.getFirst(FieldKey.ARTIST);
                if (a != null && !a.isBlank()) d.setArtistName(a);
                d.setGenre(emptyToNull(tag.getFirst(FieldKey.GENRE)));
                String bpm = tag.getFirst(FieldKey.BPM);
                if (bpm != null && !bpm.isBlank()) {
                    try { d.setBpm(Integer.parseInt(bpm.trim())); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("No se pudieron extraer metadatos del audio en staging {}: {}", audioPath, e.getMessage());
        }
        // Carátula embebida → /assets/covers (idempotente). Si no hay, queda null.
        d.setPicture_url(coverExtractor.extractAndStore(audioPath.toAbsolutePath().normalize().toString()));
        // Sugerencia de artista existente por nombre.
        if (d.getArtistName() != null && !d.getArtistName().isBlank()) {
            artistRepository.findByNameIgnoreCase(d.getArtistName().trim())
                    .ifPresent(a -> d.setSuggestedArtistId(a.getId()));
        }
        return d;
    }

    // =====================================================================
    // Fase 2 — commit
    // =====================================================================

    /** Mueve los audios de staging a selfpotify_added y persiste las canciones. */
    public List<Song> commit(SongCommitRequest req) {
        if (req == null || req.getSongs() == null || req.getSongs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay canciones que confirmar");
        }
        Path addedDir = resolveAddedDir(req.getTargetPath());
        try {
            Files.createDirectories(addedDir);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo crear la carpeta de destino");
        }
        ensureScanned(addedDir);

        List<Song> result = new ArrayList<>();
        List<String> usedTokens = new ArrayList<>();
        for (SongCommitRequest.Item item : req.getSongs()) {
            Path staged = stagingDir().resolve(item.getStagingToken()).resolve(item.getFileName());
            if (!Files.exists(staged)) {
                log.warn("Audio de staging no encontrado, se omite: {}", staged);
                continue;
            }
            Path target = uniqueTarget(addedDir, staged.getFileName().toString());
            try {
                Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                try {
                    Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error("No se pudo mover el audio confirmado a {}", target, e);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "No se pudo guardar el archivo " + item.getFileName());
                }
            }
            String finalPath = target.toAbsolutePath().normalize().toString();
            Song song = new Song();
            song.setTitle(item.getTitle() != null && !item.getTitle().isBlank()
                    ? item.getTitle().trim() : stripExt(item.getFileName()));
            song.setGenre(emptyToNull(item.getGenre()));
            song.setBpm(item.getBpm());
            song.setDuration_ms(item.getDuration_ms());
            song.setPicture_url(emptyToNull(item.getPicture_url()));
            song.setSongPath(finalPath);
            song.setAvailable(true);
            Artist artist = resolveArtist(item.getArtistId(), item.getNewArtistName());
            if (artist != null) {
                song.setArtists(List.of(artist));
            }
            result.add(songRepository.save(song));
            usedTokens.add(item.getStagingToken());
        }

        usedTokens.forEach(this::cleanupStaging);

        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo confirmar ninguna canción (¿expiró el staging?)");
        }
        log.info("Commit de subida: {} canciones añadidas en {}", result.size(), addedDir);
        return result;
    }

    /** Carpeta selfpotify_added por defecto (en la raíz de datos), para mostrar en el panel. */
    public Path defaultAddedDir() {
        return configService.addedSongsDir();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Artist resolveArtist(Long artistId, String newArtistName) {
        if (artistId != null) {
            return artistRepository.findById(artistId).orElse(null);
        }
        if (newArtistName != null && !newArtistName.isBlank()) {
            String name = newArtistName.trim();
            return artistRepository.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Artist a = new Artist();
                        a.setName(name);
                        log.info("Creando artista desde subida del panel: {}", name);
                        return artistRepository.save(a);
                    });
        }
        return null;
    }

    /**
     * Resuelve la carpeta {@code selfpotify_added} destino. Sin targetPath: la
     * carpeta de datos (siempre escribible). Con targetPath: debe ser una ruta de
     * escaneo configurada y escribible (en Docker, /music es read-only y falla).
     */
    private Path resolveAddedDir(String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            return configService.addedSongsDir();
        }
        Path base = Paths.get(targetPath).toAbsolutePath().normalize();
        boolean configured = configService.getConfig().getScan().getPaths().stream()
                .anyMatch(sp -> Paths.get(sp).toAbsolutePath().normalize().equals(base));
        if (!configured) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ruta destino debe ser una de las carpetas de música configuradas");
        }
        if (!Files.isDirectory(base) || !Files.isWritable(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ruta destino no es escribible: " + base
                            + " (en Docker el volumen de música se monta de solo lectura)");
        }
        return base.resolve("selfpotify_added");
    }

    private void ensureScanned(Path addedDir) {
        Path normalized = addedDir.toAbsolutePath().normalize();
        boolean covered = configService.getConfig().getScan().getPaths().stream()
                .anyMatch(sp -> normalized.startsWith(Paths.get(sp).toAbsolutePath().normalize()));
        if (!covered) {
            configService.addScanPath(normalized.toString());
            log.info("Carpeta de subidas {} añadida a las rutas de escaneo", normalized);
        }
    }

    private void cleanupStaging(String token) {
        try {
            Path dir = stagingDir().resolve(token);
            if (Files.isDirectory(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                }
            }
        } catch (IOException e) {
            log.warn("No se pudo limpiar el staging {}", token);
        }
    }

    private Path stagingDir() {
        return configService.dataDir().resolve("selfpotify_staging");
    }

    private boolean isAllowed(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String sanitizeFilename(String original) {
        String name = Paths.get(original).getFileName().toString();
        String cleaned = name.replaceAll("[^a-zA-Z0-9._ ()\\-]", "_").trim();
        return cleaned.isBlank() ? "audio.mp3" : cleaned;
    }

    private Path uniqueTarget(Path dir, String filename) {
        Path candidate = dir.resolve(filename);
        if (!Files.exists(candidate)) return candidate;
        int dot = filename.lastIndexOf('.');
        String stem = dot > 0 ? filename.substring(0, dot) : filename;
        String ext = dot > 0 ? filename.substring(dot) : "";
        for (int i = 1; i < 10_000; i++) {
            Path next = dir.resolve(stem + " (" + i + ")" + ext);
            if (!Files.exists(next)) return next;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Demasiados archivos con el mismo nombre");
    }

    private String stripExt(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private NameParts parseFileName(String fileName) {
        String base = stripExt(fileName);
        int sep = base.indexOf(" - ");
        if (sep > 0) {
            String artist = base.substring(0, sep).trim();
            String title = base.substring(sep + 3).trim();
            return new NameParts(artist.isBlank() ? null : artist,
                    title.isBlank() ? base.trim() : title);
        }
        return new NameParts(null, base.trim());
    }

    private record NameParts(String artist, String title) {}

    /** Resuelve un Optional de artista por id (para reasignación de artista de canción). */
    public Optional<Artist> findArtist(long id) {
        return artistRepository.findById(id);
    }
}
