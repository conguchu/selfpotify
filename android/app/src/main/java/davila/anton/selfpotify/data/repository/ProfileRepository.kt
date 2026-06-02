package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Perfil del usuario autenticado. De momento solo lectura (`GET /api/me`). */
class ProfileRepository(private val session: SessionStore) {

    suspend fun me(): Result<UserSummaryDTO> = withContext(Dispatchers.IO) {
        runCatching { api().me() }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
