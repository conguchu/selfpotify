package anton.davila.selfpotify.controllers.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body del {@code PUT /api/me/profile}. Solo se actualizan campos editables
 * del perfil (de momento, el {@code name} visible). El username y el password
 * se gestionan por otros endpoints.
 *
 * <p>Si {@code name} viene en blanco se interpreta como "borrar el nombre" y
 * el perfil queda solo con el username como identidad visible.
 */
@Data
public class ProfileUpdateRequest {
    @Size(max = 64)
    private String name;
}
