package anton.davila.selfpotify.controllers.dto;

import lombok.Data;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/artists/{id}/split}: separa un artista mal etiquetado
 * (p. ej. "Ill Pekeño / Ergo Pro") en los artistas reales cuyos nombres teclea el
 * administrador. Cada nombre se resuelve contra Last.fm (igual que el escaneo) y
 * todas las canciones y álbumes del artista original se atribuyen a TODOS los
 * resultantes antes de borrar el original.
 */
@Data
public class SplitArtistRequest {
    private List<String> names;
}
