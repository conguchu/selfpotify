package anton.davila.selfpotify.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta unificada del endpoint {@code GET /api/search}.
 *
 * <p>Misma forma siempre, con dos modos de uso:
 * <ul>
 *   <li><b>{@code type=all}</b> (default, para el dropdown global): se rellenan
 *       <em>todas</em> las categorías recortadas a {@code size} elementos cada
 *       una; {@code totalElements} por categoría refleja el total real, de modo
 *       que el cliente puede mostrar "ver más" por sección.</li>
 *   <li><b>{@code type=songs|artists|albums|playlists|users|genres}</b> (página
 *       dedicada): solo se rellena esa categoría con el slice de la página
 *       pedida; el resto vienen como {@code null} (excluidas del JSON).</li>
 * </ul>
 *
 * Los campos {@code page}/{@code size} ecoan los parámetros de la petición, no
 * son por categoría: pertenecen al modo. En {@code all} solo orientan el corte.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponseDTO {

    /** Consulta normalizada que se ejecutó (lowercased + sin diacríticos). */
    private String query;
    /** Modo de la respuesta: "all" o una de las categorías. */
    private String type;
    /** Página solicitada (0-based). */
    private int page;
    /** Tamaño solicitado. */
    private int size;

    private CategoryPage<SongDTO> songs;
    private CategoryPage<ArtistDTO> artists;
    private CategoryPage<AlbumDTO> albums;
    private CategoryPage<PlaylistDTO> playlists;
    private CategoryPage<UserSummaryDTO> users;
    private CategoryPage<GenreResultDTO> genres;

    /**
     * Slice paginado por categoría. Misma semántica que {@code Page} de Spring
     * pero sin acoplar el contrato a sus campos internos.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryPage<T> {
        /** Elementos del slice (≤ {@code size}). */
        private List<T> content;
        /** Total de elementos que satisfacen la búsqueda en esa categoría. */
        private long totalElements;
        /** Total de páginas dado {@code size}, o 1 si {@code totalElements==0}. */
        private int totalPages;
    }
}
