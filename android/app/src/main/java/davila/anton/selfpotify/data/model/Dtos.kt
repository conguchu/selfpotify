package davila.anton.selfpotify.data.model

/**
 * DTOs de la API de Selfpotify usados por el flujo de login.
 * La forma refleja `API-doc.md` (raíz del monorepo); no se inventan campos.
 */

/** Respuesta de `GET /api/config/public` (público, sin auth). */
data class PublicConfig(
    val branding: Branding? = null,
    val setupComplete: Boolean = false,
    val lastfmEnabled: Boolean = false,
    val musicLibraryPath: String? = null,
    val logoMaxBytes: Long = 0,
)

/** Sub-objeto `branding` de la config pública. `colors` son los 14 tokens CSS dinámicos. */
data class Branding(
    val appName: String? = null,
    val logoUrl: String? = null,
    val colors: Map<String, String>? = null,
)

/** Body de `POST /api/auth/login` y `POST /api/auth/signup`. */
data class LoginRequest(
    val username: String,
    val password: String,
)

/** Respuesta de `POST /api/auth/login`. */
data class JwtResponse(
    val token: String,
    val type: String? = null,
    val username: String,
    val roles: List<String>? = null,
)
