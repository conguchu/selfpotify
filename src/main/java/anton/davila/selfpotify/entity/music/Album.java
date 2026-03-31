package anton.davila.selfpotify.entity.music;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
    private List<Artist> artists;
    // un album puede tener varias canciones
    private List<Song> songs;

}
