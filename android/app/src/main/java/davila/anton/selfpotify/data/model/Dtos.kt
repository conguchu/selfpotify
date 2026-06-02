package davila.anton.selfpotify.data.model

import com.google.gson.annotations.SerializedName

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

/**
 * Canción. Forma de `SongDTO` (API-doc §8). El JSON usa snake_case en `duration_ms` y
 * `picture_url` (heredado de la entidad), de ahí los [SerializedName].
 */
data class SongDTO(
    val id: Long,
    val title: String? = null,
    @SerializedName("duration_ms") val durationMs: Long = 0,
    val genre: String? = null,
    val bpm: Int = 0,
    @SerializedName("picture_url") val pictureUrl: String? = null,
    val artistIds: List<Long>? = null,
    val artistNames: List<String>? = null,
    val listeners: Long = 0,
    val albumId: Long? = null,
) {
    /** Texto de artistas listo para mostrar (varios artistas → separados por coma). */
    val artistsLabel: String get() = artistNames?.joinToString(", ").orEmpty()
}

/** Artista. Forma de `ArtistDTO` (API-doc §8). */
data class ArtistDTO(
    val id: Long,
    val name: String? = null,
    val biography: String? = null,
    val photoUrl: String? = null,
    val albumIds: List<Long>? = null,
    val songIds: List<Long>? = null,
)

/** Playlist. Forma de `PlaylistDTO` (API-doc §8). */
data class PlaylistDTO(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val isPublic: Boolean = false,
    val creatorId: Long? = null,
    val songIds: List<Long>? = null,
    val collaboratorIds: List<Long>? = null,
)

/** Vista pública mínima de un usuario. Forma de `UserSummaryDTO` (API-doc §8). */
data class UserSummaryDTO(
    val id: Long,
    val username: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val type: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowedByMe: Boolean? = null,
)

/** Respuesta de `POST /api/listen/token`: UUID de stream token (sin claims). */
data class StreamTokenResponse(
    val token: String,
)
