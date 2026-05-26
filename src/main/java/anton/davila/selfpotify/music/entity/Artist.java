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
    private String picture_path;

    // Identificador estable de MusicBrainz resuelto vía Last.fm durante el escaneo.
    // Permite emparejar variantes de escritura del mismo artista (p. ej. "El Alfa",
    // "✅EL ALFA EL JEFE") en una única fila. Puede ser null si Last.fm no lo conoce.
    @Column(unique = true)
    private String mbid;

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
        this.setPicture_path(a.getPicture_path());
    }
}
