package anton.davila.selfpotify.music.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    public void copy(Album a) {
        this.setName(a.getName());
        this.setDuration_ms(a.getDuration_ms());
        this.setPicture_url(a.getPicture_url());
        this.setArtists(a.getArtists());
        this.setSongs(a.getSongs());
    }

}
