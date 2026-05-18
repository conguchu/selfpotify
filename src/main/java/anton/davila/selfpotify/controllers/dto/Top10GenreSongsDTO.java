package anton.davila.selfpotify.controllers.dto;

import anton.davila.selfpotify.music.entity.Song;
import lombok.Data;

import java.util.List;

@Data
public class Top10GenreSongsDTO {
    private String genre;
    private List<Song> top;
}
