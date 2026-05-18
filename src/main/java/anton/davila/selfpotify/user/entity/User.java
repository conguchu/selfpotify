package anton.davila.selfpotify.user.entity;


import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.profile.entity.Profile;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("USER")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @OneToOne
    @JoinColumn(name = "feed_id")
    private UserFeed userFeed;

    public void copy(User u) {
        this.setUsername(u.getUsername());
        this.setPassword(u.getPassword());
        this.setProfile(u.getProfile());
    }
}
