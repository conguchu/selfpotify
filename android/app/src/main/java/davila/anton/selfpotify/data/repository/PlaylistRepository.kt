package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fuente de verdad de playlists. De momento la app lista las propias y añade o quita canciones
 * desde el reproductor (bottom sheet "añadir a playlist").
 */
class PlaylistRepository(private val session: SessionStore) {

    /** Playlists del usuario autenticado (privadas y públicas). */
    suspend fun myPlaylists(): Result<List<PlaylistDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().myPlaylists() }
    }

    /** Añade [songId] a la playlist [playlistId]. Idempotente en el backend. */
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long): Result<PlaylistDTO> =
        withContext(Dispatchers.IO) {
            runCatching { api().addSongToPlaylist(playlistId, songId) }
        }

    /** Quita [songId] de la playlist [playlistId]. Devuelve la playlist actualizada. */
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long): Result<PlaylistDTO> =
        withContext(Dispatchers.IO) {
            runCatching { api().removeSongFromPlaylist(playlistId, songId) }
        }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
