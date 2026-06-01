package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

/**
 * Cuerpo de {@code PUT /api/albums/{id}}: edición manual de un álbum desde el
 * panel. Solo expone los campos editables a mano —nombre y portada— para no tocar
 * por accidente las asociaciones (artistas, canciones) al recibir un body parcial.
 */
@Data
public class AlbumUpdateRequest {
    private String name;
    /** URL de la portada (ruta {@code /assets/...} o URL externa de un CDN). */
    private String photoUrl;
}
