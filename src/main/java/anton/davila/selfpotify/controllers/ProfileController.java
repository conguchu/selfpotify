package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.config.ConfigService;
import anton.davila.selfpotify.controllers.dto.ProfileUpdateRequest;
import anton.davila.selfpotify.controllers.dto.UserSummaryDTO;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.follow.service.FollowService;
import anton.davila.selfpotify.user.profile.entity.Profile;
import anton.davila.selfpotify.user.repository.UserRepository;
import anton.davila.selfpotify.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * API del perfil del usuario autenticado y consulta pública de perfiles ajenos.
 *
 * <p>El controlador cubre dos casos:
 * <ul>
 *   <li><b>{@code /api/me/**}</b>: lecturas y modificaciones sobre el propio
 *       perfil del usuario en sesión. Cualquier rol autenticado.</li>
 *   <li><b>{@code GET /api/users/{id}/public}</b>: vista pública mínima
 *       ({@link UserSummaryDTO}) de cualquier usuario, pensada para enlazar
 *       desde los resultados de búsqueda a una página de perfil de terceros.
 *       Las playlists públicas asociadas se siguen consultando vía
 *       {@code /api/playlists/user/{userId}}.</li>
 * </ul>
 *
 * <p>La subida de avatar sigue exactamente el mismo patrón que la portada de
 * playlists ({@code PlaylistController.uploadCover}): multipart con campo
 * {@code file}, recorte centrado al cuadrado con {@code ImageIO}, salida JPEG
 * y almacenamiento idempotente bajo {@code assets/avatars/<sha256>.jpg}
 * servido por el handler {@code /assets/**}. El payload del perfil
 * ({@code PUT /api/me/profile}) deliberadamente no incluye la imagen para
 * mantener el endpoint JSON limpio.
 */
@Slf4j
@RestController
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class ProfileController {

    /** Tamaño máximo de la imagen de avatar (5 MB, igual que el cover de playlist). */
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    /** MIMEs admitidos para el avatar. Igual que el cover de playlist. */
    private static final Set<String> ACCEPTED_MIME =
            Set.of("image/jpeg", "image/png", "image/webp");

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfigService configService;

    @Autowired
    private FollowService followService;

    // =====================================
    // ----- Mi perfil
    // =====================================

    /** Devuelve la vista pública del usuario autenticado, con sus counts de follow. */
    @GetMapping("/api/me")
    public UserSummaryDTO me() {
        return enrich(getCurrentUser(), null);
    }

    /**
     * Actualiza los campos editables del perfil del usuario autenticado. Crea
     * el {@link Profile} la primera vez (cascade ALL en {@code User.profile}).
     * Trata el {@code name} en blanco como "limpiar el nombre" para que el
     * cliente pueda revertir a la identidad por defecto (username).
     */
    @PutMapping("/api/me/profile")
    @Transactional
    public UserSummaryDTO updateMyProfile(@RequestBody(required = false) ProfileUpdateRequest req) {
        User user = getCurrentUser();
        Profile profile = ensureProfile(user);
        if (req != null) {
            String name = req.getName();
            profile.setName(name == null || name.isBlank() ? null : name.trim());
        }
        // El cascade ALL persiste el Profile al guardar el User. Necesario la
        // primera vez (cuando profile aún no estaba enlazado) y barato siempre.
        userRepository.save(user);
        return enrich(user, null);
    }

    /**
     * Sube una nueva foto de perfil. Igual que {@code uploadCover} de playlist:
     * valida tipo MIME y tamaño, recorta al cuadrado por el centro y guarda el
     * resultado como JPEG en {@code assets/avatars/<sha256>.jpg}. La URL
     * resultante ({@code /assets/avatars/...}) se persiste en
     * {@link Profile#getPictureUrl()}.
     */
    @PostMapping("/api/me/profile/picture")
    @Transactional
    public ResponseEntity<?> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        User user = getCurrentUser();

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("El archivo no puede estar vacío");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_MIME.contains(contentType)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Solo se aceptan JPEG, PNG o WebP");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body("Archivo demasiado grande (máx 5 MB)");
        }

        try {
            byte[] bytes = file.getBytes();
            String sha256 = sha256Hex(bytes);

            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return ResponseEntity.badRequest().body("No se pudo leer la imagen");
            }
            int size = Math.min(original.getWidth(), original.getHeight());
            int x = (original.getWidth() - size) / 2;
            int y = (original.getHeight() - size) / 2;
            BufferedImage cropped = original.getSubimage(x, y, size, size);

            Path avatarsDir = configService.assetsDir().resolve("avatars");
            Files.createDirectories(avatarsDir);
            Path target = avatarsDir.resolve(sha256 + ".jpg");
            ImageIO.write(cropped, "jpg", target.toFile());

            Profile profile = ensureProfile(user);
            profile.setPictureUrl("/assets/avatars/" + sha256 + ".jpg");
            userRepository.save(user);

            return ResponseEntity.ok(enrich(user, null));
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("No se pudo procesar el avatar de {}", user.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la imagen: " + e.getMessage());
        }
    }

    /**
     * Quita la foto de perfil dejándola a {@code null}. No borra el fichero
     * físico de {@code assets/avatars/} porque su nombre es el SHA-256 del
     * contenido y podría estar referenciado por otro usuario que subiera la
     * misma imagen; la operación es idempotente.
     */
    @DeleteMapping("/api/me/profile/picture")
    @Transactional
    public UserSummaryDTO deleteMyAvatar() {
        User user = getCurrentUser();
        Profile profile = ensureProfile(user);
        profile.setPictureUrl(null);
        userRepository.save(user);
        return enrich(user, null);
    }

    // =====================================
    // ----- Perfil público de cualquier usuario
    // =====================================

    /**
     * Vista pública mínima de un usuario por id. Misma forma que la que
     * devuelve la búsqueda, para que el cliente pueda reusar el componente.
     * No se exponen ni contraseña ni feed.
     */
    @GetMapping("/api/users/{id}/public")
    public ResponseEntity<UserSummaryDTO> getPublicProfile(@PathVariable Long id) {
        User viewer = getCurrentUser();
        return userService.getById(id)
                .map(target -> enrich(target, viewer))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================
    // ----- Helpers
    // =====================================

    /**
     * Rellena el {@link UserSummaryDTO} con counts de follow y, si
     * {@code viewer} no es el propio target, con {@code isFollowedByMe}.
     * Para listados grandes el {@code FollowController} usa la versión batch
     * de {@link FollowService}; aquí basta con la versión por usuario.
     */
    private UserSummaryDTO enrich(User user, User viewer) {
        UserSummaryDTO dto = UserSummaryDTO.fromEntity(user);
        dto.setFollowersCount(followService.followersCountFor(user.getId()));
        dto.setFollowingCount(followService.followingCountFor(user.getId()));
        if (viewer != null && !viewer.getId().equals(user.getId())) {
            dto.setIsFollowedByMe(followService.isFollowing(viewer.getId(), user.getId()));
        }
        return dto;
    }

    private Profile ensureProfile(User user) {
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            user.setProfile(profile);
        }
        return profile;
    }

    private String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay usuario autenticado");
        }
        Object principal = auth.getPrincipal();
        String username = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();
        return userService.getByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario autenticado no encontrado: " + username));
    }
}
