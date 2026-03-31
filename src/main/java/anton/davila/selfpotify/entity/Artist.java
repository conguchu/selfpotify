package anton.davila.selfpotify.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Artist {
    @Id
    private Long id;

    private String name;
    private int listeners;
    private String picture_path;

    // un artista puede tener varios albumes. un album puede estar hecho por varios artistas
    private List<Album> albums;
    // un album tiene varias canciones.
    private List<Song> songs;
}
