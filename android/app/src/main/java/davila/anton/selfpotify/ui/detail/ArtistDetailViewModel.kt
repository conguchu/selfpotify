package davila.anton.selfpotify.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.DetailRepository
import davila.anton.selfpotify.data.repository.PlaylistRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de detalle de artista (foto/nombre + sus top tracks). */
data class ArtistDetailUiState(
    val artist: ArtistDTO? = null,
    val tracks: List<SongDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    // Hoja "añadir a playlist": id de la canción para la que está abierta (null = cerrada).
    val sheetSongId: Long? = null,
    val playlists: List<PlaylistDTO> = emptyList(),
    val playlistsLoading: Boolean = false,
    val playlistsError: Boolean = false,
)

class ArtistDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)
    private val playlistRepo = PlaylistRepository(session)

    private val _state = MutableStateFlow(ArtistDetailUiState())
    val state: StateFlow<ArtistDetailUiState> = _state.asStateFlow()

    private var loadedId: Long? = null

    /** Carga el artista [id] una sola vez (idempotente ante recomposiciones). */
    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            val artistDeferred = async { repo.artist(id) }
            val tracksDeferred = async { repo.artistTopTracks(id) }
            artistDeferred.await()
                .onSuccess { artist ->
                    val tracks = tracksDeferred.await().getOrDefault(emptyList()).distinctBy { it.id }
                    _state.update { it.copy(artist = artist, tracks = tracks, loading = false) }
                }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    fun play(index: Int) {
        viewModelScope.launch { PlaybackConnection.playFrom(_state.value.tracks, index) }
    }

    /** Abre la hoja "añadir a playlist" para [songId] y refresca las playlists propias. */
    fun openAddToPlaylist(songId: Long) {
        _state.update { it.copy(sheetSongId = songId) }
        loadPlaylists()
    }

    fun closeAddToPlaylist() {
        _state.update { it.copy(sheetSongId = null) }
    }

    private fun loadPlaylists() {
        _state.update { it.copy(playlistsLoading = true, playlistsError = false) }
        viewModelScope.launch {
            playlistRepo.myPlaylists()
                .onSuccess { pls -> _state.update { it.copy(playlistsLoading = false, playlists = pls) } }
                .onFailure { _state.update { it.copy(playlistsLoading = false, playlistsError = true) } }
        }
    }

    /**
     * Añade [songId] a [playlist] si no está, o lo quita si ya estaba. Sustituye la playlist en el
     * estado por la versión devuelta por el servidor para que el check de la hoja se refresque.
     */
    fun toggleInPlaylist(playlist: PlaylistDTO, songId: Long) {
        val wasIn = playlist.songIds?.contains(songId) == true
        viewModelScope.launch {
            val result =
                if (wasIn) playlistRepo.removeSongFromPlaylist(playlist.id, songId)
                else playlistRepo.addSongToPlaylist(playlist.id, songId)
            result.onSuccess { updated ->
                _state.update { s ->
                    s.copy(playlists = s.playlists.map { if (it.id == updated.id) updated else it })
                }
            }
        }
    }
}
