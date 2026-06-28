package anton.davila.selfpotify.music.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity
@Data
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // autocalcular
    private int duration_ms;
    private String picture_url;

    // un album puede tener varios artistas
    // @JsonIgnore: evita la recursión infinita al serializar Song -> album -> artists -> ...
    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "album_artist",
        joinColumns = @JoinColumn(name = "album_id"),
        inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<Artist> artists;
    // un album puede tener varias canciones
    // @JsonIgnore: evita la recursión infinita al serializar Song -> album -> songs -> ...
    // @BatchSize: la búsqueda carga los álbumes con sus artistas vía @EntityGraph,
    // pero 'songs' no cabe en ese grafo (dos List/bag → MultipleBagFetchException);
    // el lote agrupa el acceso lazy a las canciones de varios álbumes en pocas
    // consultas IN(...) en vez de una por álbum.
    @JsonIgnore
    @OneToMany(mappedBy = "album")
    @BatchSize(size = 100)
    private List<Song> songs;

    public void copy(Album a) {
        this.setName(a.getName());
        this.setDuration_ms(a.getDuration_ms());
        this.setPicture_url(a.getPicture_url());
        this.setArtists(a.getArtists());
        this.setSongs(a.getSongs());
    }

}
