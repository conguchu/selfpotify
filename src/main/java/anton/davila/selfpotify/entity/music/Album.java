package anton.davila.selfpotify.entity.music;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Album {
    @Id
    private Long id;
    private String name;
    // autocalcular
    private int duration_ms;
    private String picture_url;

    // un album puede tener varios artistas
    @ManyToMany
    @JoinTable(
        name = "album_artist",
        joinColumns = @JoinColumn(name = "album_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists;
    // un album puede tener varias canciones
    @OneToMany(mappedBy = "album")
    private List<Song> songs;

}
