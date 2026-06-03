package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de verdad de la pantalla Descubrir: descubrimientos diarios (con scroll infinito vía
 * canciones aleatorias), artistas recomendados y carruseles por género. Devuelve [Result] para
 * propagar errores sin lanzar a la UI (CLAUDE.md §2).
 */
class MusicRepository(private val session: SessionStore) {

    /** Descubrimientos diarios del usuario (hasta 9 canciones, estables por día). */
    suspend fun dailyDiscoveries(): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().dailyDiscoveries() }
    }

    /** Canciones aleatorias del catálogo, para alargar el carrusel de descubrimientos diarios. */
    suspend fun randomSongs(count: Int = 10): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().randomSongs(count) }
    }

    /** Artistas recomendados del home (carrusel de artistas). */
    suspend fun homeFeed(): Result<List<ArtistDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().homeFeed() }
    }

    /** Géneros escuchados recientemente, para construir un carrusel por género. */
    suspend fun recentGenres(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching { api().recentGenres() }
    }

    /** Top canciones de un [genre] (lista vacía si el género no tiene canciones). */
    suspend fun genreTopSongs(genre: String): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().genreTopSongs(genre).top.orEmpty() }
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
