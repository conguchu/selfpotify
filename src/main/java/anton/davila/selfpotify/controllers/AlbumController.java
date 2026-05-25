package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.AlbumDTO;
import anton.davila.selfpotify.music.entity.Album;
import anton.davila.selfpotify.music.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<AlbumDTO> getAll() {
        return albumService.getAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<AlbumDTO> getById(@PathVariable Long id) {
        return albumService.getById(id)
                .map(album -> ResponseEntity.ok(convertToDTO(album)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AlbumDTO create(@RequestBody Album album) {
        return convertToDTO(albumService.add(album));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AlbumDTO> update(@PathVariable Long id, @RequestBody Album albumDetails) {
        try {
            return ResponseEntity.ok(convertToDTO(albumService.update(id, albumDetails)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            albumService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private AlbumDTO convertToDTO(Album album) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId());
        dto.setName(album.getName());
        dto.setPictureUrl(album.getPicture_url());
        if (album.getSongs() != null) {
            dto.setSongIds(album.getSongs().stream().map(s -> s.getId()).collect(Collectors.toList()));
        }
        return dto;
    }
}
