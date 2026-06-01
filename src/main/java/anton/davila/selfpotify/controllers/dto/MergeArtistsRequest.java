package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/artists/merge}: unifica varios artistas duplicados
 * (p. ej. "El alfa" y "El Alfa") en uno solo. El superviviente ({@code survivorId},
 * que debe estar entre {@code ids}) conserva su id y su MBID; absorbe las canciones
 * y álbumes del resto, que se borran. Opcionalmente se renombra al superviviente
 * con {@code name}.
 */
@Data
public class MergeArtistsRequest {
    /** Ids de los artistas a unificar (incluido el superviviente). Mínimo dos. */
    private List<Long> ids;
    /** Id del artista que sobrevive a la unión. Debe estar en {@code ids}. */
    private Long survivorId;
    /** Nombre final del superviviente; si es nulo o vacío, conserva el suyo. */
    private String name;
}
