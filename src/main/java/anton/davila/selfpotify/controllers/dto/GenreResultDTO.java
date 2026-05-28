package anton.davila.selfpotify.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entrada de género en los resultados de búsqueda. Incluye el nombre del género
 * (cadena tal cual aparece en {@code Song.genre}) y cuántas canciones del
 * catálogo lo tienen, para que el frontend pueda ordenarlo o filtrarlo sin un
 * round-trip adicional.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreResultDTO {
    private String name;
    private long songCount;
}
