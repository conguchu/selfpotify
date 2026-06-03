package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.PlaylistInput
import davila.anton.selfpotify.data.model.ShareLinkResponse
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fuente de verdad de playlists: listar (propias y compartidas), crear, editar, borrar, subir
 * carátula, compartir por magic link, gestionar colaboradores y añadir/quitar canciones. Devuelve
 * [Result] para propagar errores sin lanzar a la UI (CLAUDE.md §2).
 */
class PlaylistRepository(private val session: SessionStore) {

    /** Playlists del usuario autenticado (privadas y públicas). */
    suspend fun myPlaylists(): Result<List<PlaylistDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().myPlaylists() }
    }

    /** Playlists compartidas conmigo (soy colaborador, no creador). */
    suspend fun sharedPlaylists(): Result<List<PlaylistDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().sharedPlaylists() }
    }

    /** Crea una playlist con los metadatos de [input]. */
    suspend fun create(input: PlaylistInput): Result<PlaylistDTO> = withContext(Dispatchers.IO) {
        runCatching { api().createPlaylist(input) }
    }

    /** Edita los metadatos de la playlist [id]. */
    suspend fun update(id: Long, input: PlaylistInput): Result<PlaylistDTO> =
        withContext(Dispatchers.IO) {
            runCatching { api().updatePlaylist(id, input) }
        }

    /** Borra la playlist [id]. */
    suspend fun delete(id: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api().deletePlaylist(id)
            if (!response.isSuccessful) error("HTTP ${response.code()}")
        }
    }

    /**
     * Sube la carátula de la playlist [id]. Recibe los [bytes] ya leídos del `Uri` (la lectura con
     * `ContentResolver` se hace en el ViewModel) más su [mime] y [fileName].
     */
    suspend fun uploadCover(
        id: Long,
        bytes: ByteArray,
        mime: String,
        fileName: String,
    ): Result<PlaylistDTO> = withContext(Dispatchers.IO) {
        runCatching {
            val part = MultipartBody.Part.createFormData(
                "file",
                fileName,
                bytes.toRequestBody(mime.toMediaTypeOrNull()),
            )
            api().uploadPlaylistCover(id, part)
        }
    }

    /** Genera un magic link de un solo uso para compartir la playlist [id]. */
    suspend fun shareLink(id: Long): Result<ShareLinkResponse> = withContext(Dispatchers.IO) {
        runCatching { api().sharePlaylist(id) }
    }

    /**
     * Canjea el magic link [token]: añade al usuario como colaborador y devuelve la playlist.
     * Lo dispara el deep link `selfpotify://playlist/share/{token}` recibido por la Activity.
     */
    suspend fun redeem(token: String): Result<PlaylistDTO> = withContext(Dispatchers.IO) {
        runCatching { api().redeemShareLink(token) }
    }

    /** Colaboradores actuales de la playlist [id]. */
    suspend fun collaborators(id: Long): Result<List<UserSummaryDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().playlistCollaborators(id) }
    }

    /** Quita al colaborador [userId] de la playlist [id]. */
    suspend fun removeCollaborator(id: Long, userId: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api().removeCollaborator(id, userId)
                if (!response.isSuccessful) error("HTTP ${response.code()}")
            }
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
