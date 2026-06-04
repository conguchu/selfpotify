package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.service.SongService;
import anton.davila.selfpotify.security.StreamTokenService;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.listen.service.UserSongListenService;
import anton.davila.selfpotify.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600,
        exposedHeaders = {"Content-Range", "Accept-Ranges", "Content-Length"})
@RestController
@RequestMapping("/api/listen")
public class StreamingController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSongListenService userSongListenService;

    @Autowired
    private SongService songService;

    @Autowired
    private StreamTokenService streamTokenService;

    /**
     * Emite un stream token con TTL de 4 horas, reutilizable durante toda la sesión.
     * El cliente lo pasa como {@code ?st=} en la URL de streaming para que el elemento
     * {@code <audio>} (web) o Media3 (Android) no necesiten incluir el JWT en un
     * query param visible en logs.
     *
     * <p>El TTL de 4 h cubre una sesión de escucha completa sin requerir renovación
     * continua: el reproductor reutiliza el mismo token en todas las peticiones HTTP
     * Range (seeks) y en el cambio de pista, lo que evita un POST {@code /token} por
     * cada nueva canción. El token no autentica ante ningún otro endpoint.
     */
    @PostMapping("/token")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> issueStreamToken() {
        String username = getCurrentUser().getUsername();
        return ResponseEntity.ok(Map.of("token", streamTokenService.issue(username)));
    }

    @GetMapping("{songId}")
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable String songId,
            @RequestParam(name = "st", required = false) String streamToken,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        String username = streamTokenService.validate(streamToken);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Song> s = songService.getById(Long.parseLong(songId));

        if (s.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Song song = s.get();

        if (!songService.isPathAvailable(song)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el archivo de la canción");
        }

        User currentUser = userService.getByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Path filePath = Paths.get(song.getSongPath());

        try {
            long fileSize = Files.size(filePath);
            String mimeType = detectMimeType(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl("no-store");

            if (rangeHeader != null && !rangeHeader.isBlank()) {
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                if (ranges.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                            .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                            .build();
                }

                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(fileSize);
                long end = range.getRangeEnd(fileSize);
                long contentLength = end - start + 1;

                if (start == 0) {
                    recordPlaybackListen(currentUser, song);
                }

                headers.setContentLength(contentLength);
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileSize);

                StreamingResponseBody body = outputStream -> {
                    try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                        raf.seek(start);
                        byte[] buffer = new byte[8192];
                        long remaining = contentLength;
                        int bytesRead;
                        while (remaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            remaining -= bytesRead;
                        }
                    }
                };

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .headers(headers)
                        .body(body);
            }

            recordPlaybackListen(currentUser, song);

            headers.setContentLength(fileSize);
            StreamingResponseBody fullBody = outputStream -> {
                try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = raf.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            };

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fullBody);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al transmitir audio: " + e.getMessage(), e);
        }
    }

    private void recordPlaybackListen(User currentUser, Song song) {
        userService.registerGenreListen(currentUser.getId(), song.getGenre());
        userSongListenService.recordListen(currentUser.getId(), song.getId());
    }

    private String detectMimeType(Path file) {
        String name = file.toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        String ext = (dot >= 0) ? name.substring(dot) : "";
        return switch (ext) {
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".ogg" -> "audio/ogg";
            case ".flac" -> "audio/flac";
            case ".aac" -> "audio/aac";
            default -> "application/octet-stream";
        };
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userService.getByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
