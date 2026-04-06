package anton.davila.selfpotify.entity.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
@DiscriminatorValue("ADMIN")
public class Admin extends User {
}
