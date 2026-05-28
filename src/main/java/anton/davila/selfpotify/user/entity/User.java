package anton.davila.selfpotify.user.entity;


import anton.davila.selfpotify.user.feed.entity.UserFeed;
import anton.davila.selfpotify.user.profile.entity.Profile;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("USER")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cascade ALL: al persistir/actualizar el User se persiste el Profile asociado
    // automáticamente. Útil para que el endpoint PUT /api/me/profile pueda crear
    // el Profile bajo demanda la primera vez que el usuario edita su perfil sin
    // tener que tocar el ProfileRepository explícitamente.
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private Profile profile;

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // todo usuario tiene obligatoriamente un único feed, creado junto al usuario
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "feed_id", nullable = false)
    private UserFeed userFeed;

    /** Garantiza que todo usuario se persista con un UserFeed asociado. */
    @PrePersist
    private void ensureUserFeed() {
        if (this.userFeed == null) {
            this.userFeed = new UserFeed();
        }
    }

    public void copy(User u) {
        this.setUsername(u.getUsername());
        this.setPassword(u.getPassword());
        this.setProfile(u.getProfile());
    }
}
