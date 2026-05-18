package anton.davila.selfpotify.user.feed.entity;

import anton.davila.selfpotify.music.entity.Artist;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class UserFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // un feed recomienda varios artistas; un artista puede aparecer en varios feeds
    @ManyToMany
    private List<Artist> recommendedArtists = new ArrayList<>();

    public void copy(UserFeed f) {
        this.setRecommendedArtists(f.getRecommendedArtists());
    }
}
