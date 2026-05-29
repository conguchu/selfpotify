package anton.davila.selfpotify.music.entity;

import anton.davila.selfpotify.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Vínculo de colaboración entre una {@link Playlist} y un {@link User} que no es
 * su creador: el usuario fue añadido como colaborador al canjear un magic link
 * ({@link PlaylistShareToken}).
 *
 * <p>Modelado como tabla cruzada explícita (igual que
 * {@link anton.davila.selfpotify.user.follow.entity.UserFollow}) en lugar de
 * como {@code @ManyToMany} en {@code Playlist}: así Hibernate no carga la lista
 * entera de colaboradores al leer una playlist y podemos guardar metadatos como
 * {@code created_at}.
 *
 * <p>La unique key {@code (playlist_id, user_id)} garantiza que un usuario no
 * pueda figurar dos veces como colaborador de la misma playlist.
 */
@Entity
@Table(
        name = "playlist_collaborator",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_playlist_collaborator_playlist_user",
                columnNames = {"playlist_id", "user_id"}
        ),
        indexes = {
                @Index(name = "ix_playlist_collaborator_playlist", columnList = "playlist_id"),
                @Index(name = "ix_playlist_collaborator_user", columnList = "user_id")
        }
)
@Data
public class PlaylistCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
