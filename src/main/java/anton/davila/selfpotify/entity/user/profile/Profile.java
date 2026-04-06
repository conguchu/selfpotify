package anton.davila.selfpotify.entity.user.profile;

import anton.davila.selfpotify.entity.music.Song;
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

}
