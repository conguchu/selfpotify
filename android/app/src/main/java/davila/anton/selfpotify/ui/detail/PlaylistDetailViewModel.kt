package davila.anton.selfpotify.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.DetailRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de detalle de playlist (nombre/descripción + sus canciones). */
data class PlaylistDetailUiState(
    val playlist: PlaylistDTO? = null,
    val songs: List<SongDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

class PlaylistDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)

    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    private var loadedId: Long? = null

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            repo.playlist(id)
                .onSuccess { (playlist, songs) ->
                    _state.update {
                        it.copy(playlist = playlist, songs = songs.distinctBy { s -> s.id }, loading = false)
                    }
                }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    fun play(index: Int) {
        viewModelScope.launch { PlaybackConnection.playFrom(_state.value.songs, index) }
    }
}
