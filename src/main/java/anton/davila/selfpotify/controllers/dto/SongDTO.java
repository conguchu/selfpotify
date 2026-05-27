package anton.davila.selfpotify.controllers.dto;

import anton.davila.selfpotify.music.entity.Song;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Construye un {@link SongDTO} a partir de la entidad y su número de
     * escuchas (derivado de {@code user_song_listen}). Aplana artistas y álbum
     * a sus ids. Fuente única de este mapeo para todos los controllers.
     *
     * @param song      entidad a mapear (sus artistas/álbum se acceden lazy)
     * @param listeners número de escuchas derivado de la canción
     * @return el DTO equivalente
     */
    public static SongDTO fromEntity(Song song, long listeners) {
        SongDTO dto = new SongDTO();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setDuration_ms(song.getDuration_ms());
        dto.setGenre(song.getGenre());
        dto.setBpm(song.getBpm());
        dto.setListeners(listeners);
        dto.setPicture_url(song.getPicture_url());
        if (song.getAlbum() != null) {
            dto.setAlbumId(song.getAlbum().getId());
        }
        if (song.getArtists() != null) {
            dto.setArtistIds(song.getArtists().stream()
                    .map(artist -> artist.getId())
                    .collect(Collectors.toList()));
            dto.setArtistNames(song.getArtists().stream()
                    .map(artist -> artist.getName())
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
