package davila.anton.selfpotify.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.SearchResponseDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.SearchRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pestaña Búsqueda. */
data class SearchUiState(
    val query: String = "",
    val serverUrl: String? = null,
    val loading: Boolean = false,
    val error: Boolean = false,
    val response: SearchResponseDTO? = null,
)

/**
 * Búsqueda en vivo: cada pulsación actualiza [query] inmediatamente (para que el campo de texto
 * responda) y, con un retardo de [DEBOUNCE_MS], dispara una llamada a `/api/search`. `collectLatest`
 * cancela la búsqueda en curso si llega una consulta nueva, así que solo se pinta la última.
 */
class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = SearchRepository(session)

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    /** Texto crudo de la barra; alimenta el debounce. */
    private val queryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
        }
        observeQuery()
    }

    /** Llamado por la UI en cada cambio del campo de texto. */
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    /** Limpia la barra y los resultados. */
    fun clear() {
        _state.update { it.copy(query = "", response = null, loading = false, error = false) }
        queryFlow.value = ""
    }

    /** Reproduce desde [index] usando [queue] (la lista de canciones del resultado) como cola. */
    fun play(queue: List<SongDTO>, index: Int) {
        viewModelScope.launch { PlaybackConnection.playFrom(queue, index) }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() {
        viewModelScope.launch {
            queryFlow
                .debounce(DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collectLatest { q ->
                    if (q.isEmpty()) {
                        _state.update { it.copy(loading = false, error = false, response = null) }
                        return@collectLatest
                    }
                    _state.update { it.copy(loading = true, error = false) }
                    repo.searchAll(q)
                        .onSuccess { resp -> _state.update { it.copy(loading = false, response = resp) } }
                        .onFailure { _state.update { it.copy(loading = false, error = true) } }
                }
        }
    }

    private companion object {
        /** Retardo del debounce: espera a que el usuario deje de teclear antes de llamar a la API. */
        const val DEBOUNCE_MS = 300L
    }
}
