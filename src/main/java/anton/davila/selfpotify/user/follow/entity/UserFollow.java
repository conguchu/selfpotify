package anton.davila.selfpotify.user.follow.entity;

import anton.davila.selfpotify.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Arista del grafo de seguimiento: {@code follower} sigue a {@code followed}.
 *
 * <p>Modelado como tabla cruzada explícita (igual que
 * {@link anton.davila.selfpotify.user.listen.entity.UserSongListen}) en lugar
 * de como {@code @ManyToMany} en {@code User}: así Hibernate no carga la lista
 * entera de seguidores/seguidos al leer un usuario, y los conteos de
 * popularidad ({@code countByFollowed_Id}, etc.) se derivan por consulta.
 *
 * <p>La unique key {@code (follower_id, followed_id)} garantiza que no haya
 * filas duplicadas. La columna {@code created_at} se rellena en
 * {@link #prePersist()} y se mantiene por si en el futuro queremos ordenar la
 * lista de seguidores por "más recientes primero" o exponer "te sigue desde".
 */
@Entity
@Table(
        name = "user_follow",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_follow_follower_followed",
                columnNames = {"follower_id", "followed_id"}
        ),
        indexes = {
                @Index(name = "ix_user_follow_followed", columnList = "followed_id"),
                @Index(name = "ix_user_follow_follower", columnList = "follower_id")
        }
)
@Data
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "followed_id", nullable = false)
    private User followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
