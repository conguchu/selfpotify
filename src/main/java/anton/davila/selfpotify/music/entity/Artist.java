package anton.davila.selfpotify.music.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int listeners;
    private String picture_path;

    // un artista puede tener varios albumes. un album puede estar hecho por varios artistas
    // @JsonIgnore: evita la recursión infinita al serializar Song -> artists -> albums -> ...
    @JsonIgnore
    @ManyToMany(mappedBy = "artists")
    private List<Album> albums;
    // un artista puede tener varias canciones. una cancion puede tener varios artistas
    // @JsonIgnore: evita la recursión infinita al serializar Song -> artists -> songs -> ...
    @JsonIgnore
    @ManyToMany(mappedBy = "artists")
    private List<Song> songs;

    public void copy(Artist a) {
        this.setName(a.getName());
        this.setListeners(a.getListeners());
        this.setPicture_path(a.getPicture_path());
    }
}
