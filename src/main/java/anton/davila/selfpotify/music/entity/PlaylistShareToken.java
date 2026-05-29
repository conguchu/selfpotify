package anton.davila.selfpotify.music.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Magic link de un solo uso para invitar a colaborar en una {@link Playlist}.
 *
 * <p>El creador de la playlist genera uno de estos tokens; cualquier otro
 * usuario autenticado puede canjearlo una sola vez para convertirse en
 * colaborador. El "un solo uso" se garantiza eliminando la fila al canjearla,
 * por lo que no existe campo {@code used}. No hay caducidad temporal: mientras
 * la fila exista, el token es válido.
 *
 * <p>El {@code token} es un valor aleatorio no adivinable generado con
 * {@code SecureRandom} y codificado en Base64 URL-safe.
 */
@Entity
@Table(
        name = "playlist_share_token",
        indexes = {
                @Index(name = "ix_playlist_share_token_playlist", columnList = "playlist_id")
        }
)
@Data
public class PlaylistShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, updatable = false)
    private String token;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
