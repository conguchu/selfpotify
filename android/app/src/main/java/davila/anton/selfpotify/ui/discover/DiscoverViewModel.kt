package davila.anton.selfpotify.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.MusicRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla Descubrir. */
data class DiscoverUiState(
    val songs: List<SongDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: Boolean = false,
)

class DiscoverViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = MusicRepository(session)

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
        }
        load()
    }

    /** Carga inicial: descubrimientos diarios. */
    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            repo.dailyDiscoveries()
                .onSuccess { songs -> _state.update { it.copy(songs = songs, loading = false) } }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    /** Scroll infinito: añade un lote de canciones aleatorias al final del carrusel. */
    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.songs.isEmpty()) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            repo.randomSongs(10)
                .onSuccess { more ->
                    _state.update { it.copy(songs = it.songs + more, loadingMore = false) }
                }
                .onFailure { _state.update { it.copy(loadingMore = false) } }
        }
    }

    /** Reproduce desde la canción [index], usando la lista actual como cola. */
    fun play(index: Int) {
        viewModelScope.launch {
            PlaybackConnection.playFrom(_state.value.songs, index)
        }
    }
}
