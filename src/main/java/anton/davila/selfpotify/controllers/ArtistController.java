package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.SongDTO;
import anton.davila.selfpotify.controllers.dto.Top10ArtistTracksDTO;
import anton.davila.selfpotify.controllers.dto.ArtistDTO;
import anton.davila.selfpotify.music.entity.Artist;
import anton.davila.selfpotify.music.service.ArtistService;
import anton.davila.selfpotify.music.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private SongService songService;

    // todo: getAllByListeners con paginación (los 10 más escuchados) para añadirlos a la pantalla en una sección

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<ArtistDTO> getAll() {
        return artistService.getAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ArtistDTO> getById(@PathVariable Long id) {
        return artistService.getById(id)
                .map(artist -> ResponseEntity.ok(convertToDTO(artist)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/top-10-tracks")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Top10ArtistTracksDTO> getTopTracks(@PathVariable Long id) {
        try {
            // Una única consulta agrupada para la popularidad de todas las
            // canciones (evita el N+1 de contar escuchas por cada fila).
            Map<Long, Long> listenCounts = songService.getListenCountsBySong();
            List<SongDTO> tracks = artistService.getTop10SongsById(id).stream()
                    .map(song -> SongDTO.fromEntity(song, listenCounts.getOrDefault(song.getId(), 0L)))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(new Top10ArtistTracksDTO(tracks));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ArtistDTO create(@RequestBody Artist artist) {
        return convertToDTO(artistService.add(artist));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ArtistDTO> update(@PathVariable Long id, @RequestBody Artist artistDetails) {
        try {
            return ResponseEntity.ok(convertToDTO(artistService.update(id, artistDetails)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            artistService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ArtistDTO convertToDTO(Artist artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId());
        dto.setName(artist.getName());
        dto.setPhotoUrl(artist.getPicture_path());
        if (artist.getAlbums() != null) {
            dto.setAlbumIds(artist.getAlbums().stream().map(a -> a.getId()).collect(Collectors.toList()));
        }
        if (artist.getSongs() != null) {
            dto.setSongIds(artist.getSongs().stream().map(s -> s.getId()).collect(Collectors.toList()));
        }
        return dto;
    }
}
