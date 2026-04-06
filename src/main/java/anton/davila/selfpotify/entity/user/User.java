package anton.davila.selfpotify.entity.user;

import anton.davila.selfpotify.entity.user.profile.Profile;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Data
public class User {
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;
}
