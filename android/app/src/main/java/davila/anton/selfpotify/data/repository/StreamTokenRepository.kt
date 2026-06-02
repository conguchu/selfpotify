package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.network.ApiProvider
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Emite y cachea el **stream token** usado para reproducir audio (README, "stream tokens";
 * API-doc §6). El token es un UUID de corta vida que se pasa como `?st=` en `/api/listen/{id}`;
 * NO es el JWT y no autentica ningún otro endpoint.
 *
 * Es reutilizable dentro de su TTL (4 h en el servidor): el reproductor lo necesita para las
 * múltiples peticiones HTTP Range que genera cada canción. Se cachea en memoria con un margen
 * de seguridad y se renueva de forma segura ante accesos concurrentes.
 */
class StreamTokenRepository(private val session: SessionStore) {

    private val mutex = Mutex()
    private var cached: String? = null
    private var expiresAtMs: Long = 0L

    /** Devuelve un stream token válido, reutilizando el cacheado mientras no caduque. */
    suspend fun streamToken(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            mutex.withLock {
                val now = System.currentTimeMillis()
                cached?.let { if (now < expiresAtMs) return@withLock it }
                val fresh = api().streamToken().token
                cached = fresh
                expiresAtMs = now + TTL_MS
                fresh
            }
        }
    }

    /** Construye la URL absoluta de streaming de una canción con el stream token incluido. */
    suspend fun listenUrl(songId: Long): Result<String> = streamToken().mapCatching { token ->
        val server = session.current().serverUrl ?: error("No server configured")
        "${ServerUrl.canonical(server)}/api/listen/$songId?st=$token"
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))

    private companion object {
        // 3 h 30 m: por debajo del TTL real (4 h) para no usar un token a punto de expirar.
        const val TTL_MS = 3L * 60 * 60 * 1000 + 30L * 60 * 1000
    }
}
