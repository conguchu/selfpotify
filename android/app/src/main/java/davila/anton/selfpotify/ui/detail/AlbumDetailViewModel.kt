package davila.anton.selfpotify.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.AlbumDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.DetailRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de detalle de álbum (carátula/nombre + sus canciones). */
data class AlbumDetailUiState(
    val album: AlbumDTO? = null,
    val songs: List<SongDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
)

class AlbumDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)

    private val _state = MutableStateFlow(AlbumDetailUiState())
    val state: StateFlow<AlbumDetailUiState> = _state.asStateFlow()

    private var loadedId: Long? = null

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            repo.album(id)
                .onSuccess { (album, songs) ->
                    _state.update {
                        it.copy(album = album, songs = songs.distinctBy { s -> s.id }, loading = false)
                    }
                }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    fun play(index: Int) {
        viewModelScope.launch { PlaybackConnection.playFrom(_state.value.songs, index) }
    }
}
