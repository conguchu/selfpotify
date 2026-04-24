package anton.davila.selfpotify.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlaylistDTO {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("isPublic")
    private boolean isPublic;

    private Long creatorId;
    private List<Long> songIds;
}
