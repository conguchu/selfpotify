package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Top10ArtistTracksDTO {
    // Canciones aplanadas a SongDTO, con su popularidad DERIVADA (escuchas
    // contadas sobre user_song_listen) ya resuelta, igual que el resto de
    // listados. Antes exponía la entidad Song cruda sin campo de escuchas.
    private List<SongDTO> tracks;
}
