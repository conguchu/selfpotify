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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del bottom sheet "añadir a playlist". */
data class AddToPlaylistState(
    val loading: Boolean = false,
    val playlists: List<PlaylistDTO> = emptyList(),
    val error: Boolean = false,
)

/** Resultado puntual de añadir a una playlist (para un mensaje efímero). */
enum class AddResult { ADDED, FAILED }

/**
 * ViewModel del reproductor (mini-player y pantalla completa). La UI solo habla con el player a
 * través de aquí; este delega en el singleton [PlaybackConnection] (MVVM, CLAUDE.md §2). También
 * gestiona la acción "añadir a playlist" desde el reproductor.
 */
class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val playlistRepo = PlaylistRepository(SessionStore(app))

    val state: StateFlow<PlayerState> = PlaybackConnection.state

    private val _addState = MutableStateFlow(AddToPlaylistState())
    val addState: StateFlow<AddToPlaylistState> = _addState.asStateFlow()

    private val _addResult = MutableSharedFlow<AddResult>(extraBufferCapacity = 1)
    val addResult: SharedFlow<AddResult> = _addResult.asSharedFlow()

    fun togglePlay() = PlaybackConnection.togglePlay()
    fun next() = PlaybackConnection.next()
    fun previous() = PlaybackConnection.previous()
    fun toggleRepeat() = PlaybackConnection.toggleRepeat()
    fun seekTo(positionMs: Long) = PlaybackConnection.seekTo(positionMs)

    /** Carga las playlists propias para mostrarlas en el bottom sheet. */
    fun loadPlaylists() {
        _addState.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            playlistRepo.myPlaylists()
                .onSuccess { pls -> _addState.update { it.copy(loading = false, playlists = pls) } }
                .onFailure { _addState.update { it.copy(loading = false, error = true) } }
        }
    }

    /** Añade la canción actualmente en reproducción a la playlist elegida. */
    fun addCurrentToPlaylist(playlistId: Long) {
        val songId = state.value.songId ?: return
        viewModelScope.launch {
            playlistRepo.addSongToPlaylist(playlistId, songId)
                .onSuccess { _addResult.emit(AddResult.ADDED) }
                .onFailure { _addResult.emit(AddResult.FAILED) }
        }
    }
}
