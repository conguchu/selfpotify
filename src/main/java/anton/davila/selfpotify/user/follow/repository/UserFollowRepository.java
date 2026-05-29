package anton.davila.selfpotify.user.follow.repository;

import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.follow.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso a la tabla {@code user_follow}. Las firmas {@code _Id} de Spring Data
 * navegan la {@code @ManyToOne} sin tener que hidratar el {@link User} en el
 * servicio (los IDs ya vienen en los DTOs y los path params).
 */
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    boolean existsByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    Optional<UserFollow> findByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    long countByFollowed_Id(Long followedId);

    long countByFollower_Id(Long followerId);

    /** Usuarios que siguen a {@code followedId}, más recientes primero. */
    List<UserFollow> findByFollowed_IdOrderByCreatedAtDesc(Long followedId);

    /** Usuarios a los que sigue {@code followerId}, más recientes primero. */
    List<UserFollow> findByFollower_IdOrderByCreatedAtDesc(Long followerId);

    // -----------------------------------------------------------------
    // Consultas batch para evitar N+1 al pintar listas grandes
    // -----------------------------------------------------------------

    /**
     * Subconjunto de {@code candidateIds} que {@code followerId} ya sigue.
     * Útil para rellenar {@code isFollowedByMe} en una lista de DTOs con una
     * sola consulta.
     */
    @Query("select uf.followed.id from UserFollow uf " +
            "where uf.follower.id = :followerId and uf.followed.id in :candidateIds")
    Set<Long> findFollowedIdsByFollowerAmong(
            @Param("followerId") Long followerId,
            @Param("candidateIds") Collection<Long> candidateIds);

    /** {@code (followedId, totalFollowers)} agrupado, para batch de counts. */
    @Query("select uf.followed.id, count(uf) from UserFollow uf " +
            "where uf.followed.id in :userIds group by uf.followed.id")
    List<Object[]> countFollowersGrouped(@Param("userIds") Collection<Long> userIds);

    /** {@code (followerId, totalFollowing)} agrupado, para batch de counts. */
    @Query("select uf.follower.id, count(uf) from UserFollow uf " +
            "where uf.follower.id in :userIds group by uf.follower.id")
    List<Object[]> countFollowingGrouped(@Param("userIds") Collection<Long> userIds);

    /**
     * Borra todas las aristas en las que un usuario aparezca como follower o
     * followed. Pensada para {@code UserService.delete} y {@code ResetService}
     * para no chocar con la FK.
     */
    @Modifying
    @Query("delete from UserFollow uf where uf.follower.id = :userId or uf.followed.id = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);
}
