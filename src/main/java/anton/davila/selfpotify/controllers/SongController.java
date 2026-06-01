package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.ImportRequest;
import anton.davila.selfpotify.controllers.dto.SongArtistsRequest;
import anton.davila.selfpotify.controllers.dto.SongCommitRequest;
import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.controllers.dto.SongDraftDTO;
import anton.davila.selfpotify.controllers.dto.Top10GenreSongsDTO;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.service.SongService;
import anton.davila.selfpotify.music.service.SongUploadService;
import anton.davila.selfpotify.music.service.external.EmbeddedCoverExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/songs")
public class SongController {


    // todo: getAllByListeners con paginación (los 10 más escuchados) para añadirlos a la pantalla en una sección

    @Autowired
    private SongService songService;

    @Autowired
    private SongUploadService songUploadService;

    @Autowired
    private EmbeddedCoverExtractor coverExtractor;

    private static final Map<String, String> ACCEPTED_IMAGE_MIME = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/webp", "webp"
    );

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<SongDTO> getAllSongs() {
        // Una única consulta agrupada para la popularidad de todas las
        // canciones (evita el N+1 de contar escuchas por cada fila).
        Map<Long, Long> listenCounts = songService.getListenCountsBySong();
        return songService.getAll().stream()
                .map(song -> convertToDTO(song, listenCounts.getOrDefault(song.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SongDTO> getSongById(@PathVariable("id") Long id) {
        return songService.getById(id)
                .map(song -> ResponseEntity.ok(convertToDTO(song, songService.getListenCount(song.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    // El género llega como query param (no path) porque algunos géneros contienen
    // '/' (p.ej. "Rap/Hip Hop") y la barra codificada en la ruta hace que Tomcat
    // rechace la petición con 400.
    @GetMapping("/top")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Top10GenreSongsDTO> getTopGenreSongs(@RequestParam("genre") String genre) {
        List<Song> topSongs = songService.getTop10ByGenre(genre);
        if (topSongs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<Long, Long> listenCounts = songService.getListenCountsBySong();
        List<SongDTO> top = topSongs.stream()
                .map(song -> convertToDTO(song, listenCounts.getOrDefault(song.getId(), 0L)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new Top10GenreSongsDTO(genre, top));
    }

    @GetMapping("/random")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<SongDTO> getRandomSongs(@RequestParam(defaultValue = "10") int count) {
        Map<Long, Long> listenCounts = songService.getListenCountsBySong();
        return songService.getRandomSongs(Math.min(count, 50)).stream()
                .map(song -> convertToDTO(song, listenCounts.getOrDefault(song.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SongDTO createSong(@RequestBody Song song) {
        // For simplicity, receiving Song entity but we could use SongDTO.
        // Una canción recién creada todavía no tiene escuchas registradas.
        return convertToDTO(songService.add(song), 0L);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SongDTO> updateSong(@PathVariable("id") Long id, @RequestBody Song songDetails) {
        try {
            Song updatedSong = songService.update(id, songDetails);
            return ResponseEntity.ok(convertToDTO(updatedSong, songService.getListenCount(updatedSong.getId())));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSong(@PathVariable("id") Long id) {
        try {
            songService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SongDTO> importFolder(@RequestBody ImportRequest request) {
        String raw = request == null ? null : request.getPath();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo 'path' es obligatorio");
        }
        Path folder = Paths.get(raw);
        if (!Files.exists(folder) || !Files.isDirectory(folder) || !Files.isReadable(folder)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ruta no existe, no es un directorio o no es legible: " + raw);
        }
        // Canciones recién importadas: aún sin escuchas registradas.
        return songService.loadFolder(folder.toAbsolutePath().toString()).stream()
                .map(song -> convertToDTO(song, 0L))
                .collect(Collectors.toList());
    }

    /**
     * Fase 1 de la subida drag&drop: guarda los audios en staging y devuelve sus
     * metadatos extraídos como borradores editables, SIN incorporarlos aún a la
     * biblioteca. El panel los revisa/ajusta y luego confirma con {@code /commit}.
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SongDraftDTO> uploadSongs(@RequestParam("files") List<MultipartFile> files) {
        return songUploadService.uploadToStaging(files);
    }

    /**
     * Fase 2: confirma una subida en staging con los metadatos ya ajustados. Mueve
     * los audios a selfpotify_added (volumen de datos en Docker, o ruta elegida en
     * local) y persiste las canciones. Ver {@link SongUploadService}.
     */
    @PostMapping(value = "/commit", consumes = "application/json")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SongDTO> commitSongs(@RequestBody SongCommitRequest request) {
        return songUploadService.commit(request).stream()
                .map(song -> convertToDTO(song, 0L))
                .collect(Collectors.toList());
    }

    /**
     * Sube una imagen de carátula y la guarda en el MISMO almacén que las carátulas
     * normales ({@code /assets/covers/<sha256>.<ext>}). Devuelve {@code { "url": ... }}.
     * La usa el panel tanto al subir canciones como al editarlas.
     */
    @PostMapping(value = "/cover", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> uploadCover(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo requerido");
        }
        String mime = file.getContentType();
        if (mime == null || !ACCEPTED_IMAGE_MIME.containsKey(mime)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato de imagen no soportado: " + mime + ". Aceptados: " + ACCEPTED_IMAGE_MIME.keySet());
        }
        try {
            String url = coverExtractor.storeImageBytes(file.getBytes(), mime);
            return Map.of("url", url);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la carátula");
        }
    }

    /** Reasigna los artistas de una canción (modal de búsqueda del panel). */
    @PutMapping("/{id}/artists")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SongDTO> setArtists(@PathVariable("id") Long id,
                                              @RequestBody SongArtistsRequest request) {
        try {
            Song updated = songService.setArtists(id, request == null ? null : request.getArtistIds());
            return ResponseEntity.ok(convertToDTO(updated, songService.getListenCount(updated.getId())));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private SongDTO convertToDTO(Song song, long listeners) {
        return SongDTO.fromEntity(song, listeners);
    }
}
