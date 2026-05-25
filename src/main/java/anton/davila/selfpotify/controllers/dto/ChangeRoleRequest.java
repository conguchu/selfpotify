package anton.davila.selfpotify.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @JsonProperty("isAdmin")
    private boolean isAdmin;
}
