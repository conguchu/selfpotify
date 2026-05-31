package anton.davila.selfpotify.music.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
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
    private int bpm;
    private String songPath;
    private boolean available = true;

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

    public void copy(Song s) {
        this.setTitle(s.getTitle());
        this.setDuration_ms(s.getDuration_ms());
        this.setGenre(s.getGenre());
        this.setBpm(s.getBpm());
        // songPath se conserva si no llega valor: la edición de metadatos desde el
        // panel no expone ni cambia la ruta física del audio (SongDTO no la incluye),
        // así un PUT sin songPath no debe borrarla.
        if (s.getSongPath() != null && !s.getSongPath().isBlank()) {
            this.setSongPath(s.getSongPath());
        }
        this.setPicture_url(s.getPicture_url());
    }

    // todo: calcular duration en mm:ss
}
