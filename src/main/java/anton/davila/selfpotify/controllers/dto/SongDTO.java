package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongDTO {
    private Long id;
    private String title;
    private int duration_ms;
    private String genre;
    private int bpm;
    private String picture_url;
    private List<Long> artistIds;
    private List<String> artistNames;
    private int listeners;
    private Long albumId;
}
