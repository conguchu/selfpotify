package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.PlaylistDTO;
import anton.davila.selfpotify.controllers.dto.ShareLinkResponse;
import anton.davila.selfpotify.controllers.dto.UserSummaryDTO;
import anton.davila.selfpotify.music.entity.Playlist;
import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.music.repository.SongRepository;
import anton.davila.selfpotify.music.service.PlaylistService;
import anton.davila.selfpotify.music.service.PlaylistSharingService;
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
import java.awt.Color;
import java.awt.Graphics2D;
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
    private PlaylistSharingService playlistSharingService;

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
                .map(p -> convertToDTO(p, true))
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
                    if (canView(playlist, currentUser)) {
                        return ResponseEntity.ok(convertToDTO(playlist, true));
                    }
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shared")
    public List<PlaylistDTO> getSharedWithMe() {
        User currentUser = getCurrentUser();
        return playlistSharingService.sharedWith(currentUser).stream()
                .map(p -> convertToDTO(p, true))
                .collect(Collectors.toList());
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
                    // conserva la portada existente si el cliente no envía una nueva
                    details.setPictureUrl(body.getPictureUrl() != null ? body.getPictureUrl() : playlist.getPictureUrl());
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
                        playlistSharingService.deleteSharingData(playlist);
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
                        // Recorte centrado aplanado a RGB sobre fondo blanco: el codificador
                        // JPEG no sabe escribir canal alfa y, si la imagen subida es un PNG/WebP
                        // con transparencia, ImageIO.write(...) devuelve false sin escribir nada.
                        BufferedImage croppedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = croppedImage.createGraphics();
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, size, size);
                        g.drawImage(originalImage, 0, 0, size, size, x, y, x + size, y + size, null);
                        g.dispose();

                        Path assetsDir = configService.assetsDir();
                        Path playlistCoversDir = assetsDir.resolve("playlist-covers");
                        Files.createDirectories(playlistCoversDir);

                        Path targetFile = playlistCoversDir.resolve(sha256 + ".jpg");
                        if (!ImageIO.write(croppedImage, "jpg", targetFile.toFile())) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("No se pudo codificar la imagen como JPEG");
                        }

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

    // ---- Playlists compartidas (colaboración vía magic link) ----

    @PostMapping("/{id}/share")
    public ResponseEntity<ShareLinkResponse> createShareLink(@PathVariable Long id) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (playlist.getCreator() == null
                            || !playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<ShareLinkResponse>build();
                    }
                    String token = playlistSharingService.createShareToken(playlist);
                    return ResponseEntity.ok(new ShareLinkResponse(token, "/api/playlists/share/" + token));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/share/{token}")
    public ResponseEntity<PlaylistDTO> redeemShareLink(@PathVariable String token) {
        User currentUser = getCurrentUser();
        Playlist playlist = playlistSharingService.redeemToken(token, currentUser);
        return ResponseEntity.ok(convertToDTO(playlist, true));
    }

    @PostMapping("/{id}/songs/{songId}")
    public ResponseEntity<PlaylistDTO> addSong(@PathVariable Long id, @PathVariable Long songId) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!canEditSongs(playlist, currentUser)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                    }
                    Song song = songRepository.findById(songId).orElse(null);
                    if (song == null) {
                        return ResponseEntity.notFound().<PlaylistDTO>build();
                    }
                    Playlist updated = playlistService.addSong(id, song);
                    return ResponseEntity.ok(convertToDTO(updated, true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<PlaylistDTO> removeSong(@PathVariable Long id, @PathVariable Long songId) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!canEditSongs(playlist, currentUser)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<PlaylistDTO>build();
                    }
                    Song song = songRepository.findById(songId).orElse(null);
                    if (song == null) {
                        return ResponseEntity.notFound().<PlaylistDTO>build();
                    }
                    Playlist updated = playlistService.removeSong(id, song);
                    return ResponseEntity.ok(convertToDTO(updated, true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<List<UserSummaryDTO>> getCollaborators(@PathVariable Long id) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (!canView(playlist, currentUser)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<List<UserSummaryDTO>>build();
                    }
                    List<UserSummaryDTO> collaborators = playlistSharingService.collaboratorsOf(playlist).stream()
                            .map(UserSummaryDTO::fromEntity)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(collaborators);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public ResponseEntity<Void> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        return playlistService.getById(id)
                .map(playlist -> {
                    User currentUser = getCurrentUser();
                    if (playlist.getCreator() == null
                            || !playlist.getCreator().getId().equals(currentUser.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
                    }
                    playlistSharingService.removeCollaborator(playlist, userId);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Puede ver: pública, o creador, o colaborador. */
    private boolean canView(Playlist playlist, User user) {
        if (playlist.isPublic()) return true;
        if (playlist.getCreator() != null && playlist.getCreator().getId().equals(user.getId())) return true;
        return playlistSharingService.isCollaborator(playlist, user);
    }

    /** Puede añadir/quitar canciones: creador o colaborador. */
    private boolean canEditSongs(Playlist playlist, User user) {
        if (playlist.getCreator() != null && playlist.getCreator().getId().equals(user.getId())) return true;
        return playlistSharingService.isCollaborator(playlist, user);
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
        return convertToDTO(playlist, false);
    }

    /**
     * @param includeCollaborators si {@code true}, puebla {@code collaboratorIds}
     *                             con una consulta extra. Se deja en {@code false}
     *                             en endpoints de listado para evitar N+1.
     */
    private PlaylistDTO convertToDTO(Playlist playlist, boolean includeCollaborators) {
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
        if (includeCollaborators) {
            dto.setCollaboratorIds(playlistSharingService.collaboratorsOf(playlist).stream()
                    .map(User::getId)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
