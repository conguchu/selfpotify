package anton.davila.selfpotify.controllers.dto;

import anton.davila.selfpotify.music.entity.Song;

import java.util.List;

public class Top10GenreSongsDTO {
    private String genre;
    private List<Song> top;
}
