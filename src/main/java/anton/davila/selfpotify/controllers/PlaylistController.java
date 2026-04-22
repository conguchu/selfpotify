package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.PlaylistDTO;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.service.PlaylistService;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my")
    public List<PlaylistDTO> getMyPlaylists() {
        User currentUser = getCurrentUser();
        return playlistService.getByUser(currentUser).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlaylistDTO>> getUserPublicPlaylists(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(
                        playlistService.getPublicByUser(user).stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList())
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistDTO> getById(@PathVariable Long id) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (playlist.isPublic() || playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.ok(convertToDTO(playlist));
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PlaylistDTO create(@RequestBody Playlist playlist) {
        playlist.setCreator(getCurrentUser());
        return convertToDTO(playlistService.add(playlist));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaylistDTO> update(@PathVariable Long id, @RequestBody Playlist playlistDetails) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                    }
                    return ResponseEntity.ok(convertToDTO(playlistService.update(id, playlistDetails)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return playlistService.getById(id)
                .map(playlist -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    User currentUser = getCurrentUser();
                    
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    
                    if (isAdmin || playlist.getCreator().getId().equals(currentUser.getId())) {
                        playlistService.delete(id);
                        return ResponseEntity.noContent().<Void>build();
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private PlaylistDTO convertToDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setPublic(playlist.isPublic());
        if (playlist.getCreator() != null) {
            dto.setCreatorId(playlist.getCreator().getId());
        }
        if (playlist.getSongs() != null) {
            dto.setSongIds(playlist.getSongs().stream().map(s -> s.getId()).collect(Collectors.toList()));
        }
        return dto;
    }
}
