package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Borrador editable de una canción subida por drag&drop, ANTES de incorporarla a
 * la biblioteca. El audio queda en una carpeta de staging (no escaneada) y estos
 * metadatos —extraídos del fichero— se devuelven al panel para que el admin los
 * revise/ajuste; al confirmar (`POST /api/songs/commit`) se persiste la canción.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SongDraftDTO {
    /** Identificador de la carpeta de staging donde quedó el audio. */
    private String stagingToken;
    /** Nombre del fichero dentro del staging (para localizarlo al confirmar). */
    private String fileName;
    private String title;
    /** Nombre de artista extraído del tag (texto, aún sin resolver a entidad). */
    private String artistName;
    /** Id del artista existente cuyo nombre coincide con el extraído, o null. */
    private Long suggestedArtistId;
    private String genre;
    private int bpm;
    private int duration_ms;
    /** Carátula embebida ya volcada a /assets/covers, o null si el audio no tenía. */
    private String picture_url;
}
