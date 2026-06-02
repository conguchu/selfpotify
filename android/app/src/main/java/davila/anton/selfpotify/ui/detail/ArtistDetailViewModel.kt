package davila.anton.selfpotify.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.DetailRepository
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
)

class ArtistDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)

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
}
