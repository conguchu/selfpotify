package anton.davila.selfpotify.user.follow.service;

import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.follow.entity.UserFollow;
import anton.davila.selfpotify.user.follow.repository.UserFollowRepository;
import anton.davila.selfpotify.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lógica de seguimiento entre usuarios sobre la tabla {@code user_follow}.
 *
 * <p>El servicio expone tanto las primitivas (seguir, dejar de seguir, contar,
 * listar) como utilidades de <em>enriquecimiento</em>
 * ({@link #followersCountFor(Long)}, {@link #buildIsFollowedSet}…) que los
 * controladores usan para rellenar el {@code UserSummaryDTO} con counts y la
 * flag {@code isFollowedByMe} en una sola consulta batch por listado.
 *
 * <p>Decisión: el servicio nunca lanza si intentas seguir a quien ya sigues o
 * dejar de seguir a quien no sigues — se devuelve el estado actual sin error.
 * El controlador respondería en cualquier caso con el {@code UserSummaryDTO}
 * actualizado del usuario seguido, de modo que el cliente puede tratar la
 * petición como idempotente. La única validación dura es "no puedes seguirte
 * a ti mismo".
 */
@Slf4j
@Service
public class FollowService {

    @Autowired
    private UserFollowRepository repo;

    @Autowired
    private UserRepository userRepository;

    // =====================================
    // ----- Mutaciones
    // =====================================

    /**
     * Crea la arista {@code follower -> followed} si no existía.
     * Idempotente: si ya existía, no hace nada.
     *
     * @throws IllegalArgumentException si los ids son iguales
     */
    @Transactional
    public void follow(User follower, User followed) {
        if (follower == null || followed == null) {
            throw new IllegalArgumentException("follower/followed no pueden ser null");
        }
        if (Objects.equals(follower.getId(), followed.getId())) {
            throw new IllegalArgumentException("No te puedes seguir a ti mismo");
        }
        if (repo.existsByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())) {
            return;
        }
        UserFollow edge = new UserFollow();
        edge.setFollower(follower);
        edge.setFollowed(followed);
        repo.save(edge);
        log.info("{} ahora sigue a {}", follower.getUsername(), followed.getUsername());
    }

    /** Elimina la arista si existía. {@code true} si se borró. */
    @Transactional
    public boolean unfollow(User follower, User followed) {
        if (follower == null || followed == null) return false;
        return repo.findByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())
                .map(edge -> {
                    repo.delete(edge);
                    log.info("{} dejó de seguir a {}", follower.getUsername(), followed.getUsername());
                    return true;
                })
                .orElse(false);
    }

    // =====================================
    // ----- Lecturas por usuario
    // =====================================

    public long followersCountFor(Long userId) {
        return repo.countByFollowed_Id(userId);
    }

    public long followingCountFor(Long userId) {
        return repo.countByFollower_Id(userId);
    }

    public boolean isFollowing(Long followerId, Long followedId) {
        if (followerId == null || followedId == null) return false;
        if (Objects.equals(followerId, followedId)) return false;
        return repo.existsByFollower_IdAndFollowed_Id(followerId, followedId);
    }

    /** Lista de usuarios que siguen a {@code userId}. */
    public List<User> followersOf(Long userId) {
        return repo.findByFollowed_IdOrderByCreatedAtDesc(userId).stream()
                .map(UserFollow::getFollower)
                .toList();
    }

    /** Lista de usuarios a los que sigue {@code userId}. */
    public List<User> followingOf(Long userId) {
        return repo.findByFollower_IdOrderByCreatedAtDesc(userId).stream()
                .map(UserFollow::getFollowed)
                .toList();
    }

    // =====================================
    // ----- Helpers para enriquecer DTOs (batch, evita N+1)
    // =====================================

    /**
     * Subconjunto de {@code candidateIds} que el usuario {@code viewerId} ya
     * sigue. Si {@code viewerId == null}, devuelve set vacío.
     */
    public Set<Long> buildIsFollowedSet(Long viewerId, Collection<Long> candidateIds) {
        if (viewerId == null || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> filtered = new HashSet<>(candidateIds);
        filtered.remove(viewerId); // nadie se sigue a sí mismo
        if (filtered.isEmpty()) return Collections.emptySet();
        return repo.findFollowedIdsByFollowerAmong(viewerId, filtered);
    }

    /** Mapa {@code userId -> followersCount} para todos los ids pedidos. */
    public Map<Long, Long> followersCountsFor(Collection<Long> userIds) {
        return groupedCounts(repo.countFollowersGrouped(userIds), userIds);
    }

    /** Mapa {@code userId -> followingCount} para todos los ids pedidos. */
    public Map<Long, Long> followingCountsFor(Collection<Long> userIds) {
        return groupedCounts(repo.countFollowingGrouped(userIds), userIds);
    }

    private static Map<Long, Long> groupedCounts(List<Object[]> rows, Collection<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        for (Long id : ids) result.put(id, 0L);
        for (Object[] row : rows) {
            result.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    /**
     * Borra todas las aristas que involucran al usuario, en cualquier
     * dirección. {@code UserService.delete} la llama antes de borrar el User
     * para no romper la FK.
     */
    @Transactional
    public int deleteAllInvolving(Long userId) {
        return repo.deleteAllInvolving(userId);
    }
}
