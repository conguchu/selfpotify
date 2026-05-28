package anton.davila.selfpotify.controllers;

import anton.davila.selfpotify.controllers.dto.SearchResponseDTO;
import anton.davila.selfpotify.music.service.SearchService;
import anton.davila.selfpotify.user.entity.User;
import anton.davila.selfpotify.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Barra de búsqueda global. Una sola ruta {@code GET /api/search} que cubre los
 * dos modos típicos de UI:
 *
 * <ul>
 *   <li><b>Dropdown global</b>: {@code /api/search?q=rosalia} → respuesta con
 *       {@code type=all} y hasta {@link SearchService#ALL_MODE_PER_CATEGORY}
 *       resultados por categoría. El cliente pinta una vista previa
 *       agrupada.</li>
 *   <li><b>Página dedicada</b>: {@code /api/search?q=rosalia&type=songs&page=2&size=20}
 *       → solo se rellena la categoría pedida con el slice paginado.</li>
 * </ul>
 *
 * La forma de la respuesta es la misma en ambos modos; las categorías no
 * usadas se omiten del JSON (vienen como {@code null}).
 */
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private UserService userService;

    /**
     * @param q    consulta del usuario (puede llevar acentos, mayúsculas, etc.)
     * @param type "all" (default) o {@code songs|artists|albums|playlists|users|genres}
     * @param page índice 0-based de la página
     * @param size tamaño de la página (acotado por {@link SearchService#MAX_PAGE_SIZE})
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<SearchResponseDTO> search(
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size) {
        try {
            SearchResponseDTO result = searchService.search(q, type, page, size, currentUserOrNull());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // type fuera del enum permitido → 400 con mensaje legible.
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Resuelve el usuario autenticado o {@code null} si la petición no trae
     * principal reconocible (no debería ocurrir bajo {@code @PreAuthorize}, pero
     * mantiene el servicio robusto frente a llamadas internas / tests).
     */
    private User currentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        String username = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();
        return userService.getByUsername(username).orElse(null);
    }
}
