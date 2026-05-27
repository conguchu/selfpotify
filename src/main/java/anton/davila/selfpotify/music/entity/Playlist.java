package anton.davila.selfpotify.music.entity;

import anton.davila.selfpotify.user.entity.User;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@JsonAutoDetect(isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToMany
    @JoinTable(
        name = "playlist_song",
        joinColumns = @JoinColumn(name = "playlist_id"),
        inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    private List<Song> songs;
    private int duration_ms = 0;

    @JsonProperty("isPublic")
    private boolean isPublic = false;

    @Column(name = "picture_url")
    private String pictureUrl;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    // constructor mediante lista de canciones
    public Playlist(List<Song> songs) {
        // calcula la duracion total de las canciones
        songs.forEach(s -> { this.duration_ms += s.getDuration_ms(); });
        // guarda la lista de canciones en el objeto
        this.songs = songs;
    }

    public void copy(Playlist p) {
        this.setName(p.getName());
        this.setDescription(p.getDescription());
        this.setSongs(p.getSongs());
        this.setDuration_ms(p.getDuration_ms());
        this.setPublic(p.isPublic());
        this.setPictureUrl(p.getPictureUrl());
    }

}
