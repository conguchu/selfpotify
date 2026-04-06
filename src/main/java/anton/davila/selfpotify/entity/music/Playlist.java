package anton.davila.selfpotify.entity.music;

import anton.davila.selfpotify.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
        name = "playlist_song",
        joinColumns = @JoinColumn(name = "playlist_id"),
        inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    private List<Song> songs;
    private int duration_ms = 0;
    private boolean isPublic = false;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    // constructor mediante lista de canciones
    public Playlist(List<Song> songs) {
        // calcula la duracion total de las canciones
        songs.forEach(s -> { this.duration_ms += s.getDuration_ms(); });
        // guarda la lista de canciones en el objeto
        this.songs = songs;
    }

}
