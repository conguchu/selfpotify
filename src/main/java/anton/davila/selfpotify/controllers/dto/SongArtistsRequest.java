package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

import java.util.List;

/** Cuerpo de {@code PUT /api/songs/{id}/artists}: reasigna los artistas de una canción. */
@Data
public class SongArtistsRequest {
    private List<Long> artistIds;
}
