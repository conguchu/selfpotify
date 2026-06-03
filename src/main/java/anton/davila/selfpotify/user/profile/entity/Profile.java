package anton.davila.selfpotify.user.profile.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Datos públicos del usuario que no pertenecen a la cuenta en sí (username,
 * password): el nombre que se ve en la UI y la foto de perfil. El username
 * sigue siendo el identificador único de login y permanece en {@link
 * anton.davila.selfpotify.user.entity.User}; el {@code name} de aquí es
 * libre, editable y puede repetirse entre usuarios.
 *
 * <p>{@code pictureUrl} guarda o bien una URL absoluta o, lo habitual, una
 * ruta relativa servida por {@code /assets/**} (p. ej.
 * {@code /assets/avatars/<sha256>.<ext>}), mismo patrón que las carátulas de
 * canciones y playlists.
 */
@Entity
@Data
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre visible del usuario. Distinto del username; puede repetirse. */
    private String name;

    /** URL de la foto de perfil ({@code /assets/avatars/...} o externa). */
    @Column(name = "picture_url")
    private String pictureUrl;

    public void copy(Profile p) {
        this.setName(p.getName());
        this.setPictureUrl(p.getPictureUrl());
    }

}
