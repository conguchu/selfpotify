package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlbumDTO {
    private Long id;
    private String name;
    private String releaseDate;
    private String pictureUrl;
    private Long artistId;
    private List<Long> songIds;
}
