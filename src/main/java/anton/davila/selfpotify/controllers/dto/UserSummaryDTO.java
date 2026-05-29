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
 *
 * <p>{@code followersCount} y {@code followingCount} se incluyen siempre
 * (default 0) para que el contrato JSON sea estable; los endpoints de perfil
 * (/api/me, /api/users/{id}/public y los de follow) los rellenan, mientras
 * que la búsqueda los deja en 0 para no introducir N+1 en ese flujo. El
 * cliente solo los pinta en el perfil.
 *
 * <p>{@code isFollowedByMe} es {@code null} cuando no hay contexto de quien
 * mira (p. ej. listados administrativos) o cuando el viewer es el propio
 * usuario; {@code true}/{@code false} en cualquier otro caso.
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
    /** Cuántos usuarios siguen a este. 0 si no se ha calculado (p. ej. en búsqueda). */
    private long followersCount;
    /** Cuántos usuarios sigue este. 0 si no se ha calculado. */
    private long followingCount;
    /** ¿Lo sigue el usuario en sesión? {@code null} si no hay contexto o es uno mismo. */
    private Boolean isFollowedByMe;

    public static UserSummaryDTO fromEntity(User user) {
        UserSummaryDTO dto = new UserSummaryDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        if (user.getProfile() != null) {
            dto.setDisplayName(user.getProfile().getName());
            dto.setAvatarUrl(user.getProfile().getPictureUrl());
        }
        dto.setType(user instanceof Admin ? "ADMIN" : "USER");
        // Los campos de follow se quedan en su default (0/null) hasta que el
        // endpoint correspondiente los rellene vía FollowService.
        return dto;
    }
}
