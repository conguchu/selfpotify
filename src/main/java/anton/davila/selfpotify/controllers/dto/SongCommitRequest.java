package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

import java.util.List;

/**
 * Confirmación de una subida en staging (`POST /api/songs/commit`): mueve cada
 * audio de la carpeta de staging a {@code selfpotify_added} y lo persiste con los
 * metadatos finales editados en el panel.
 */
@Data
public class SongCommitRequest {
    /**
     * Carpeta destino donde guardar los audios confirmados. Debe ser una de las
     * rutas de escaneo configuradas y escribible; si es null/blank se usa la
     * carpeta de datos del servidor. Se crea dentro una subcarpeta selfpotify_added.
     */
    private String targetPath;
    private List<Item> songs;

    @Data
    public static class Item {
        private String stagingToken;
        private String fileName;
        private String title;
        /** Artista existente a asignar; si es null se usa newArtistName. */
        private Long artistId;
        /** Nombre de un artista a crear (cuando artistId es null). */
        private String newArtistName;
        private String genre;
        private int bpm;
        private int duration_ms;
        private String picture_url;
    }
}
