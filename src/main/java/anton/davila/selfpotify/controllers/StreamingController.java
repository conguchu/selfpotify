package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

/**
 * Este es el controller que se encarga de darle a la librería media 3 un archivo
 * por su ID
 */
@RestController
@RequestMapping("/api/listen")
public class StreamingController {

    @Autowired
    private SongService songService;

    @GetMapping("{songId}")
    public ResponseEntity<StreamingResponseBody> stream(
            @PathVariable String songId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        // obtener la canción
        Optional<Song> s = songService.getById( Long.parseLong(songId) );

        if (s.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Song song = s.get();

        // comprobamos primero que sea accesible el archivo
        if (!songService.isPathAvailable(song)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encuentra el archivo de la canción");
        }

        Path filePath = Paths.get(song.getSongPath());

        try {
            long fileSize = Files.size(filePath);
            String mimeType = detectMimeType(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl("public, max-age=3600, immutable");

            // si hay header Range, responder con 206 Partial Content
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

            // sin Range: devolver archivo completo (200 OK)
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

}
