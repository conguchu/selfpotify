package anton.davila.selfpotify.controllers.dto;

import anton.davila.selfpotify.user.entity.Admin;
import anton.davila.selfpotify.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vista pública mínima de un usuario, pensada para la barra de búsqueda y para
 * cualquier listado donde no proceda devolver la entidad completa. Expone solo
 * lo necesario para que el cliente pueda renderizar al usuario y enlazarlo por
 * id; nunca incluye la contraseña ni el feed.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDTO {
    private Long id;
    private String username;
    /** Nombre del {@code Profile} si existe; null si el usuario aún no ha creado perfil. */
    private String displayName;
    /** URL del avatar del perfil, o null si no tiene. */
    private String avatarUrl;
    /** "ADMIN" o "USER", igual que el discriminador JPA. */
    private String type;

    public static UserSummaryDTO fromEntity(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        if (user.getProfile() != null) {
            dto.setDisplayName(user.getProfile().getName());
            dto.setAvatarUrl(user.getProfile().getPictureUrl());
        }
        dto.setType(user instanceof Admin ? "ADMIN" : "USER");
        return dto;
    }
}
