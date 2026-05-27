package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.PlaylistDTO;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.PlaylistService;
import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlists")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @Autowired
    private SongRepository songRepository;
    @Autowired
    private UserService userService;

    @Autowired
    private ConfigService configService;

    @GetMapping("/my")
    public List<PlaylistDTO> getMyPlaylists() {
        User currentUser = getCurrentUser();
        return playlistService.getByUser(currentUser).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlaylistDTO>> getUserPublicPlaylists(@PathVariable Long userId) {
        return userService.getById(userId)
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
    public ResponseEntity<PlaylistDTO> create(@RequestBody PlaylistDTO body) {
        Playlist playlist = new Playlist();
        playlist.setName(body.getName());
        playlist.setDescription(body.getDescription());
        playlist.setPublic(body.isPublic());
        playlist.setCreator(getCurrentUser());
        playlist.setSongs(resolveSongs(body.getSongIds()));
        Playlist saved = playlistService.add(playlist);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlaylistDTO> update(@PathVariable Long id, @RequestBody PlaylistDTO body) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                    }
                    Playlist details = new Playlist();
                    details.setName(body.getName());
                    details.setDescription(body.getDescription());
                    details.setPublic(body.isPublic());
                    details.setSongs(resolveSongs(body.getSongIds()));
                    return ResponseEntity.ok(convertToDTO(playlistService.update(id, details)));
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

    @PostMapping("/{id}/cover")
    public ResponseEntity<?> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }

                    if (file.isEmpty()) {
                        return ResponseEntity.badRequest().body("El archivo no puede estar vacío");
                    }

                    String contentType = file.getContentType();
                    if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
                        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Solo se aceptan JPEG, PNG o WebP");
                    }

                    long maxFileSize = 5 * 1024 * 1024;
                    if (file.getSize() > maxFileSize) {
                        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Archivo demasiado grande (máx 5 MB)");
                    }

                    try {
                        byte[] fileBytes = file.getBytes();
                        String sha256 = calculateSHA256(fileBytes);

                        BufferedImage originalImage = ImageIO.read(file.getInputStream());
                        if (originalImage == null) {
                            return ResponseEntity.badRequest().body("No se pudo leer la imagen");
                        }

                        int size = Math.min(originalImage.getWidth(), originalImage.getHeight());
                        int x = (originalImage.getWidth() - size) / 2;
                        int y = (originalImage.getHeight() - size) / 2;
                        BufferedImage croppedImage = originalImage.getSubimage(x, y, size, size);

                        Path assetsDir = configService.assetsDir();
                        Path playlistCoversDir = assetsDir.resolve("playlist-covers");
                        Files.createDirectories(playlistCoversDir);

                        Path targetFile = playlistCoversDir.resolve(sha256 + ".jpg");
                        ImageIO.write(croppedImage, "jpg", targetFile.toFile());

                        String pictureUrl = "/assets/playlist-covers/" + sha256 + ".jpg";
                        playlist.setPictureUrl(pictureUrl);
                        playlistService.update(id, playlist);

                        return ResponseEntity.ok(convertToDTO(playlist));
                    } catch (IOException | NoSuchAlgorithmException e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al procesar la imagen: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private List<Song> resolveSongs(List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(songRepository.findAllById(songIds));
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

    private PlaylistDTO convertToDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setDescription(playlist.getDescription());
        dto.setPublic(playlist.isPublic());
        dto.setPictureUrl(playlist.getPictureUrl());
        if (playlist.getCreator() != null) {
            dto.setCreatorId(playlist.getCreator().getId());
        }
        dto.setSongIds(playlist.getSongs() == null
                ? Collections.emptyList()
                : playlist.getSongs().stream().map(Song::getId).collect(Collectors.toList()));
        return dto;
    }
}
