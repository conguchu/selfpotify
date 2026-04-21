package anton.davila.selfpotify.user.profile.entity;

import anton.davila.selfpotify.music.entity.Song;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String avatarURL;

    @ManyToOne
    @JoinColumn(name = "favourite_song_id")
    private Song favouriteSong;

    public void copy(Profile p) {
        this.setName(p.getName());
        this.setAvatarURL(p.getAvatarURL());
        this.setFavouriteSong(p.getFavouriteSong());
    }

}
