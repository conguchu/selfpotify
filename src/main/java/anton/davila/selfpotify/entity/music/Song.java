package anton.davila.selfpotify.entity.music;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int duration_ms;
    private String genre;
    private int listeners;
    private int bpm;


    // una cancion puede tener varios artistas
    @ManyToMany
    @JoinTable(
        name = "song_artist",
        joinColumns = @JoinColumn(name = "song_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists;
    // una cancion está en un album. un album tiene varias canciones
    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;
    private String picture_url;

    // todo: calcular duration en mm:ss
}
