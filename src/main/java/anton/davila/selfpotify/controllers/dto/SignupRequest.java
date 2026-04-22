package anton.davila.selfpotify.controllers.dto;

import lombok.Data;
import java.util.Set;

@Data
public class SignupRequest {
    private String username;
    private String password;
    private boolean isAdmin = false;
}
