package anton.davila.selfpotify.entity.music;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Song {
    @Id
    private Long id;

    private String title;
    private int duration_ms;
    private String genre;
    private int listeners;

    // una cancion puede tener varios artistas
    private List<Artist> artists;
    // una cancion está en un album. un album tiene varias canciones
    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;
    private String picture_url;

    // todo: calcular duration en mm:ss
}
