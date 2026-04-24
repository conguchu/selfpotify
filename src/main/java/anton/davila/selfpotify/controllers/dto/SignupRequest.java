package anton.davila.selfpotify.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SignupRequest {
    private String username;
    private String password;

    @JsonProperty("isAdmin")
    private boolean isAdmin = false;
}
