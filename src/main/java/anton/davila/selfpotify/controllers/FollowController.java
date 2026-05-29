package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.UserSummaryDTO;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.follow.service.FollowService;
import anton.davila.selfpotify.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Endpoints del grafo de seguimiento (follow / unfollow / listar followers y
 * following).
 *
 * <p>El endpoint de mutación opera siempre sobre el path {@code /api/users/{id}/follow}
 * porque la <em>arista</em> que se crea o se borra es entre <strong>el usuario
 * autenticado</strong> y {@code {id}}: el {@code follower} no se pasa por path
 * para que un cliente nunca pueda escribir aristas ajenas. Las listas
 * (followers/following) sí son globales y devuelven la vista mínima
 * ({@link UserSummaryDTO}) enriquecida con counts y con la flag
 * {@code isFollowedByMe} respecto al usuario en sesión, calculadas en
 * <em>batch</em> con {@link FollowService} para no caer en N+1.
 */
@Slf4j
@RestController
@RequestMapping("/api/users/{id}")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class FollowController {

    @Autowired
    private FollowService followService;

    @Autowired
    private UserService userService;

    /** Sigue a {@code {id}}. Idempotente: si ya lo seguías, responde 200 sin cambios. */
    @PostMapping("/follow")
    public ResponseEntity<UserSummaryDTO> follow(@PathVariable Long id) {
        User me = getCurrentUser();
        User target = userService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (me.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No te puedes seguir a ti mismo");
        }
        followService.follow(me, target);
        return ResponseEntity.ok(enrich(target, me));
    }

    /** Deja de seguir a {@code {id}}. Idempotente: si no lo seguías, responde 200. */
    @DeleteMapping("/follow")
    public ResponseEntity<UserSummaryDTO> unfollow(@PathVariable Long id) {
        User me = getCurrentUser();
        User target = userService.getById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        followService.unfollow(me, target);
        return ResponseEntity.ok(enrich(target, me));
    }

    /** Quién sigue a {@code {id}}, más recientes primero. */
    @GetMapping("/followers")
    public ResponseEntity<List<UserSummaryDTO>> followers(@PathVariable Long id) {
        if (userService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User me = getCurrentUser();
        List<User> users = followService.followersOf(id);
        return ResponseEntity.ok(enrichList(users, me));
    }

    /** A quién sigue {@code {id}}, más recientes primero. */
    @GetMapping("/following")
    public ResponseEntity<List<UserSummaryDTO>> following(@PathVariable Long id) {
        if (userService.getById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User me = getCurrentUser();
        List<User> users = followService.followingOf(id);
        return ResponseEntity.ok(enrichList(users, me));
    }

    // =====================================
    // ----- Helpers
    // =====================================

    /**
     * Rellena los tres campos extra del DTO para un único usuario. Para
     * listados usar {@link #enrichList(List, User)} (batch).
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

    /** Versión batch: una consulta por categoría en vez de tres por fila. */
    private List<UserSummaryDTO> enrichList(List<User> users, User viewer) {
        if (users.isEmpty()) return List.of();
        Set<Long> ids = users.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Long> followersByUser = followService.followersCountsFor(ids);
        Map<Long, Long> followingByUser = followService.followingCountsFor(ids);
        Long viewerId = viewer == null ? null : viewer.getId();
        Set<Long> followedByViewer = followService.buildIsFollowedSet(viewerId, ids);
        return users.stream().map(u -> {
            UserSummaryDTO dto = UserSummaryDTO.fromEntity(u);
            dto.setFollowersCount(followersByUser.getOrDefault(u.getId(), 0L));
            dto.setFollowingCount(followingByUser.getOrDefault(u.getId(), 0L));
            if (viewerId != null && !viewerId.equals(u.getId())) {
                dto.setIsFollowedByMe(followedByViewer.contains(u.getId()));
            }
            return dto;
        }).toList();
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
