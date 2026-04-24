package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtistDTO {
    private Long id;
    private String name;
    private String biography;
    private String photoUrl;
    private List<Long> albumIds;
    private List<Long> songIds;
}
