package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de verdad de la pantalla Descubrir: descubrimientos diarios y canciones aleatorias
 * (scroll infinito). Devuelve [Result] para propagar errores sin lanzar a la UI (CLAUDE.md §2).
 */
class MusicRepository(private val session: SessionStore) {

    /** Descubrimientos diarios del usuario (hasta 9 canciones, estables por día). */
    suspend fun dailyDiscoveries(): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().dailyDiscoveries() }
    }

    /** Canciones aleatorias del catálogo, para alargar el carrusel sin límite. */
    suspend fun randomSongs(count: Int = 10): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().randomSongs(count) }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
