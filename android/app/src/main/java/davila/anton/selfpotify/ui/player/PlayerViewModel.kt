package davila.anton.selfpotify.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.repository.PlaylistRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import davila.anton.selfpotify.playback.PlayerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del bottom sheet "añadir a playlist". */
data class AddToPlaylistState(
    val loading: Boolean = false,
    val playlists: List<PlaylistDTO> = emptyList(),
    val error: Boolean = false,
)

/** Resultado puntual de alternar una playlist (para un mensaje efímero). */
enum class AddResult { ADDED, REMOVED, FAILED }

/**
 * ViewModel del reproductor (mini-player y pantalla completa). La UI solo habla con el player a
 * través de aquí; este delega en el singleton [PlaybackConnection] (MVVM, CLAUDE.md §2). También
 * gestiona la acción "añadir/quitar de playlist" desde el reproductor.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val playlistRepo = PlaylistRepository(SessionStore(app))

    val state: StateFlow<PlayerState> = PlaybackConnection.state

    private val _addState = MutableStateFlow(AddToPlaylistState())
    val addState: StateFlow<AddToPlaylistState> = _addState.asStateFlow()

    private val _addResult = MutableSharedFlow<AddResult>(extraBufferCapacity = 1)
    val addResult: SharedFlow<AddResult> = _addResult.asSharedFlow()

    /**
     * `true` si la canción en reproducción está en alguna playlist propia. Combina el estado del
     * player (canción actual) con las playlists cacheadas, para pintar el check del botón de
     * "añadir a playlist" sin necesidad de abrir el bottom sheet.
     */
    val currentInAnyPlaylist: StateFlow<Boolean> =
        combine(state, _addState) { player, add ->
            val songId = player.songId ?: return@combine false
            add.playlists.any { it.songIds?.contains(songId) == true }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // Carga inicial para que el botón sepa de entrada si la canción ya está en una playlist.
        loadPlaylists()
    }

    fun togglePlay() = PlaybackConnection.togglePlay()
    fun next() = PlaybackConnection.next()
    fun previous() = PlaybackConnection.previous()
    fun toggleRepeat() = PlaybackConnection.toggleRepeat()
    fun seekTo(positionMs: Long) = PlaybackConnection.seekTo(positionMs)

    /** Carga las playlists propias para mostrarlas (con su check) en el bottom sheet. */
    fun loadPlaylists() {
        _addState.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            playlistRepo.myPlaylists()
                .onSuccess { pls -> _addState.update { it.copy(loading = false, playlists = pls) } }
                .onFailure { _addState.update { it.copy(loading = false, error = true) } }
        }
    }

    /**
     * Añade la canción en curso a [playlist] si no está, o la quita si ya estaba. Sustituye la
     * playlist en el estado por la versión devuelta por el servidor para que el check se refresque.
     */
    fun toggleCurrentInPlaylist(playlist: PlaylistDTO) {
        val songId = state.value.songId ?: return
        val wasIn = playlist.songIds?.contains(songId) == true
        viewModelScope.launch {
            val result =
                if (wasIn) playlistRepo.removeSongFromPlaylist(playlist.id, songId)
                else playlistRepo.addSongToPlaylist(playlist.id, songId)
            result
                .onSuccess { updated ->
                    _addState.update { s ->
                        s.copy(playlists = s.playlists.map { if (it.id == updated.id) updated else it })
                    }
                    _addResult.emit(if (wasIn) AddResult.REMOVED else AddResult.ADDED)
                }
                .onFailure { _addResult.emit(AddResult.FAILED) }
        }
    }
}
