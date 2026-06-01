package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

/**
 * Cuerpo de {@code PUT /api/artists/{id}}: edición manual de un artista desde el
 * panel de administración. Solo expone los campos editables a mano —nombre y
 * foto— y deja fuera el MBID, que es identidad resuelta automáticamente.
 */
@Data
public class ArtistUpdateRequest {
    private String name;
    /** URL de la foto (ruta {@code /assets/...} o URL externa de un CDN). */
    private String photoUrl;
}
