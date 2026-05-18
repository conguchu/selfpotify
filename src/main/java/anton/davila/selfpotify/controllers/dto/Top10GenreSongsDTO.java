package anton.davila.selfpotify.controllers.dto;

import anton.davila.selfpotify.music.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Top10GenreSongsDTO {
    private String genre;
    private List<Song> top;
}
