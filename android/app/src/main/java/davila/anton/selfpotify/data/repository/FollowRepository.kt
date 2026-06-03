package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Grafo de seguimiento entre usuarios: listar seguidores/seguidos y seguir/dejar de seguir.
 * `follow`/`unfollow` son idempotentes en el backend y devuelven el `UserSummaryDTO` del usuario
 * objetivo con los contadores y `isFollowedByMe` recalculados. Devuelve [Result] (CLAUDE.md §2).
 */
class FollowRepository(private val session: SessionStore) {

    /** Seguidores de [id] (más recientes primero). */
    suspend fun followers(id: Long): Result<List<UserSummaryDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().followers(id) }
    }

    /** Usuarios a los que sigue [id] (más recientes primero). */
    suspend fun following(id: Long): Result<List<UserSummaryDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().following(id) }
    }

    /** Sigue a [id]. */
    suspend fun follow(id: Long): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().follow(id) }
    }

    /** Deja de seguir a [id]. */
    suspend fun unfollow(id: Long): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().unfollow(id) }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
