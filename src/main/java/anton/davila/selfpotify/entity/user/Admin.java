package anton.davila.selfpotify.entity.user;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class Admin extends User {
    // single table -> columna type (admin o user)
}
