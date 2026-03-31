package anton.davila.selfpotify.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Profile {
    @Id
    private Long id;
}
