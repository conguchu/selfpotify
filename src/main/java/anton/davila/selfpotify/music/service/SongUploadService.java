package anton.davila.selfpotify.music.service;

import anton.davila.selfpotify.config.AppProperties;
import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.config.MusicLibraryResolver;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Guarda en disco los audios subidos desde el panel de administración (drag&drop)
 * y los incorpora a la biblioteca.
 *
 * <h3>Dónde se guardan</h3>
 * Los audios subidos NO pueden ir al volumen de música, que en Docker se monta
 * read-only ({@code /music:ro}). Por eso el destino es siempre una carpeta
 * {@code selfpotify_added} escribible:
 * <ul>
 *   <li><b>Docker:</b> dentro del volumen de datos persistente
 *       ({@code /data/selfpotify/selfpotify_added}). El panel no deja elegir ruta.</li>
 *   <li><b>Local:</b> dentro de la ruta de música elegida por el admin de entre
 *       las {@code scan.paths} configuradas ({@code <ruta>/selfpotify_added}); si no
 *       se indica ninguna, se cae a la carpeta de datos como en Docker.</li>
 * </ul>
 *
 * <h3>Cómo se incorporan</h3>
 * Tras copiar los ficheros se asegura que {@code selfpotify_added} esté entre las
 * rutas de escaneo (salvo que ya quede cubierta por una ruta padre) y se lanza un
 * re-escaneo idempotente de esa carpeta, que extrae metadatos y aplica el
 * autocompletado de género/carátula igual que cualquier otra importación.
 */
@Slf4j
@Service
public class SongUploadService {

    /** Extensiones admitidas, alineadas con SongService#isAudioFile. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".mp3", ".wav");

    private final ConfigService configService;
    private final MusicLibraryResolver musicLibraryResolver;
    private final SongService songService;
    private final SongRepository songRepository;
    private final AppProperties appProperties;

    public SongUploadService(ConfigService configService,
                             MusicLibraryResolver musicLibraryResolver,
                             SongService songService,
                             SongRepository songRepository,
                             AppProperties appProperties) {
        this.configService = configService;
        this.musicLibraryResolver = musicLibraryResolver;
        this.songService = songService;
        this.songRepository = songRepository;
        this.appProperties = appProperties;
    }

    /**
     * Persiste los ficheros en {@code selfpotify_added} y devuelve las canciones
     * resultantes (recién insertadas o ya presentes con el mismo songPath).
     *
     * @param files      audios subidos (multipart)
     * @param targetPath ruta base destino (solo se respeta fuera de Docker y debe
     *                   ser una de las {@code scan.paths}); ignorada en Docker
     */
    public List<Song> uploadSongs(List<MultipartFile> files, String targetPath) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se ha subido ningún archivo");
        }

        Path addedDir = resolveAddedDir(targetPath);
        try {
            Files.createDirectories(addedDir);
        } catch (IOException e) {
            log.error("No se pudo crear la carpeta de subidas {}", addedDir, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo crear la carpeta de destino");
        }

        long maxBytes = appProperties.getUpload().getMaxFileSize().toBytes();
        List<String> savedPaths = new ArrayList<>();
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
            Path target = uniqueTarget(addedDir, sanitizeFilename(original));
            try {
                // Sin REPLACE_EXISTING: el nombre ya es único, así no pisamos audios previos.
                Files.copy(file.getInputStream(), target);
            } catch (IOException e) {
                log.error("No se pudo guardar el audio subido en {}", target, e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se pudo guardar el archivo " + original);
            }
            savedPaths.add(target.toAbsolutePath().normalize().toString());
        }

        if (savedPaths.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ningún archivo válido para subir");
        }

        ensureScanned(addedDir);
        // Importación idempotente: extrae metadatos y aplica autocompletado, sin
        // duplicar los audios ya presentes de subidas anteriores (dedupe por songPath).
        songService.rescanFolder(addedDir.toAbsolutePath().normalize().toString());

        List<Song> result = new ArrayList<>();
        for (String p : savedPaths) {
            songRepository.findFirstBySongPath(p).ifPresent(result::add);
        }
        log.info("Subida completada: {} archivos guardados en {}, {} canciones en biblioteca",
                savedPaths.size(), addedDir, result.size());
        return result;
    }

    /** Carpeta selfpotify_added por defecto (en la raíz de datos), para mostrar en el panel. */
    public Path defaultAddedDir() {
        return configService.addedSongsDir();
    }

    /**
     * Resuelve la carpeta {@code selfpotify_added} de destino. En Docker es fija
     * (volumen de datos); en local respeta la ruta elegida si es una scan path
     * válida, o cae a la carpeta de datos.
     */
    private Path resolveAddedDir(String targetPath) {
        if (musicLibraryResolver.runningInDocker()) {
            return configService.addedSongsDir();
        }
        if (targetPath != null && !targetPath.isBlank()) {
            Path base = Paths.get(targetPath).toAbsolutePath().normalize();
            boolean allowed = configService.getConfig().getScan().getPaths().stream()
                    .anyMatch(sp -> Paths.get(sp).toAbsolutePath().normalize().equals(base));
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La ruta destino debe ser una de las carpetas de música configuradas");
            }
            return base.resolve("selfpotify_added");
        }
        return configService.addedSongsDir();
    }

    /**
     * Registra {@code addedDir} como ruta de escaneo salvo que ya esté cubierta por
     * una ruta padre existente (caso local en que se elige una scan path como base).
     */
    private void ensureScanned(Path addedDir) {
        Path normalized = addedDir.toAbsolutePath().normalize();
        boolean covered = configService.getConfig().getScan().getPaths().stream()
                .anyMatch(sp -> normalized.startsWith(Paths.get(sp).toAbsolutePath().normalize()));
        if (!covered) {
            configService.addScanPath(normalized.toString());
            log.info("Carpeta de subidas {} añadida a las rutas de escaneo", normalized);
        }
    }

    private boolean isAllowed(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /** Deja solo el nombre base y sustituye caracteres problemáticos por '_'. */
    private String sanitizeFilename(String original) {
        String name = Paths.get(original).getFileName().toString();
        String cleaned = name.replaceAll("[^a-zA-Z0-9._ ()\\-]", "_").trim();
        return cleaned.isBlank() ? "audio.mp3" : cleaned;
    }

    /** Evita pisar ficheros existentes añadiendo " (n)" antes de la extensión. */
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
}
