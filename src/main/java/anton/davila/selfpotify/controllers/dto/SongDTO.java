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
    // Popularidad DERIVADA: número de escuchas de la canción contado sobre la
    // tabla de eventos user_song_listen (no es un campo almacenado en Song).
    private long listeners;
    private Long albumId;
}
