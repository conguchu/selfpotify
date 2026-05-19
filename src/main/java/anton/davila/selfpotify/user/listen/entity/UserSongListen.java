package anton.davila.selfpotify.user.listen.entity;

import anton.davila.selfpotify.music.entity.Song;
import anton.davila.selfpotify.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Tabla cruzada que registra qué usuario ha escuchado qué canción.
 * Es independiente del contador global {@code Song.listeners}: aquí cada fila
 * es un evento de escucha de un usuario concreto, no un agregado.
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "user_song_listen",
        indexes = @Index(name = "idx_usl_user_listened",
                columnList = "user_id, listenedAt"))
public class UserSongListen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "song_id")
    private Song song;

    private Instant listenedAt;

    public UserSongListen(User user, Song song) {
        this.user = user;
        this.song = song;
        this.listenedAt = Instant.now();
    }
}
