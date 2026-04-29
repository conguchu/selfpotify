package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.ImportRequest;
import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/songs")
public class SongController {


    // todo: getAllByListeners con paginación (los 10 más escuchados) para añadirlos a la pantalla en una sección

    @Autowired
    private SongService songService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<SongDTO> getAllSongs() {
        return songService.getAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SongDTO> getSongById(@PathVariable("id") Long id) {
        return songService.getById(id)
                .map(song -> ResponseEntity.ok(convertToDTO(song)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SongDTO createSong(@RequestBody Song song) {
        // For simplicity, receiving Song entity but we could use SongDTO
        return convertToDTO(songService.add(song));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SongDTO> updateSong(@PathVariable("id") Long id, @RequestBody Song songDetails) {
        try {
            Song updatedSong = songService.update(id, songDetails);
            return ResponseEntity.ok(convertToDTO(updatedSong));
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
        return songService.loadFolder(folder.toAbsolutePath().toString()).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private SongDTO convertToDTO(Song song) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setDuration_ms(song.getDuration_ms());
        dto.setGenre(song.getGenre());
        dto.setBpm(song.getBpm());
        dto.setPicture_url(song.getPicture_url());
        if (song.getAlbum() != null) {
            dto.setAlbumId(song.getAlbum().getId());
        }
        if (song.getArtists() != null) {
            dto.setArtistIds(song.getArtists().stream()
                    .map(artist -> artist.getId())
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
