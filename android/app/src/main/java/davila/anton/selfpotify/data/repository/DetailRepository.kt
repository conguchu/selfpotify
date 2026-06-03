package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.AlbumDTO
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.network.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Fuente de verdad de las pantallas de detalle (artista, álbum, playlist, usuario). Devuelve
 * [Result] para propagar errores sin lanzar a la UI (CLAUDE.md §2). Álbumes y playlists solo traen
 * `songIds`, así que se resuelven a `SongDTO` con llamadas en paralelo a `GET /api/songs/{id}`.
 */
class DetailRepository(private val session: SessionStore) {

    suspend fun artist(id: Long): Result<ArtistDTO> = withContext(Dispatchers.IO) {
        runCatching { api().getArtist(id) }
    }

    /** Top 10 canciones del artista (lista vacía si no tiene escuchas). */
    suspend fun artistTopTracks(id: Long): Result<List<SongDTO>> = withContext(Dispatchers.IO) {
        runCatching { api().artistTopTracks(id).tracks.orEmpty() }
    }

    /** Álbum + sus canciones resueltas en el orden de `songIds`. */
    suspend fun album(id: Long): Result<Pair<AlbumDTO, List<SongDTO>>> = withContext(Dispatchers.IO) {
        runCatching {
            val album = api().getAlbum(id)
            album to resolveSongs(album.songIds.orEmpty())
        }
    }

    /** Playlist + sus canciones resueltas en el orden de `songIds`. */
    suspend fun playlist(id: Long): Result<Pair<PlaylistDTO, List<SongDTO>>> = withContext(Dispatchers.IO) {
        runCatching {
            val playlist = api().getPlaylist(id)
            playlist to resolveSongs(playlist.songIds.orEmpty())
        }
    }

    /**
     * Usuario público + las playlists que le mostramos: sus **públicas** unidas a las
     * **colaborativas conmigo** (donde él es creador o colaborador y yo participo, así que llegan vía
     * `myPlaylists`/`sharedPlaylists` con `collaboratorIds` poblado ⇒ `isShared` ⇒ icono de
     * personitas). Las llamadas auxiliares se toleran a fallo para no tumbar la pantalla.
     */
    suspend fun user(id: Long): Result<Pair<UserSummaryDTO, List<PlaylistDTO>>> = withContext(Dispatchers.IO) {
        runCatching {
            val user = api().getUserPublic(id)
            val public = runCatching { api().userPlaylists(id) }.getOrDefault(emptyList())
            val mine = runCatching { api().myPlaylists() }.getOrDefault(emptyList())
            val shared = runCatching { api().sharedPlaylists() }.getOrDefault(emptyList())
            val collab = (mine + shared).filter {
                it.creatorId == id || it.collaboratorIds?.contains(id) == true
            }
            user to (public + collab).distinctBy { it.id }
        }
    }

    /**
     * Resuelve cada id a su `SongDTO` en paralelo, conservando el orden de [ids] y **descartando**
     * las que fallen (una canción borrada no debe tumbar toda la pantalla).
     */
    private suspend fun resolveSongs(ids: List<Long>): List<SongDTO> = coroutineScope {
        ids.map { id -> async { runCatching { api().getSong(id) }.getOrNull() } }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun api() =
        ApiProvider.api(session.current().serverUrl ?: error("No server configured"))
}
