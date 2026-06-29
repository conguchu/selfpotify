package anton.davila.selfpotify.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(max = 50)
    private String username;

    // @NotBlank evita el NPE de encoder.encode(null) cuando el body no trae
    // password; el límite superior corta strings desproporcionados.
    @NotBlank
    @Size(max = 100)
    private String password;

    @JsonProperty("isAdmin")
    private boolean isAdmin = false;
}
