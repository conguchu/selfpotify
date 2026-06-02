package davila.anton.selfpotify.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.MusicRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Un carrusel por género: el nombre del género y sus canciones top. */
data class GenreSection(
    val genre: String,
    val songs: List<SongDTO>,
)

/** Estado de la pantalla Descubrir (carruseles horizontales, estilo web). */
data class DiscoverUiState(
    val daily: List<SongDTO> = emptyList(),
    val artists: List<ArtistDTO> = emptyList(),
    val genres: List<GenreSection> = emptyList(),
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

    /**
     * Ids ya presentes en el carrusel diario. El scroll infinito pide canciones aleatorias que
     * pueden repetir ids ya mostrados; añadirlos provocaría claves duplicadas en el `LazyRow`
     * (crash). Se filtran aquí para que cada id aparezca una sola vez.
     */
    private val dailyIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
        }
        load()
    }

    /** Carga inicial: descubrimientos diarios, artistas recomendados y carruseles por género. */
    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            // Daily y artistas en paralelo; los géneros dependen de su lista, así que van después.
            val dailyDeferred = async { repo.dailyDiscoveries() }
            val artistsDeferred = async { repo.homeFeed() }
            val genreNamesDeferred = async { repo.recentGenres() }

            val daily = dailyDeferred.await()
            val artists = artistsDeferred.await().getOrDefault(emptyList())

            daily.onSuccess { songs ->
                // distinctBy para blindar el LazyRow ante IDs duplicados que pueda devolver la API.
                val unique = songs.distinctBy { it.id }
                dailyIds.clear()
                dailyIds.addAll(unique.map { it.id })
                _state.update { it.copy(daily = unique, artists = artists, loading = false) }
            }.onFailure {
                _state.update { it.copy(loading = false, error = true) }
            }

            // Un carrusel por género reciente; se descartan los que no devuelven canciones.
            if (daily.isSuccess) {
                val sections = genreNamesDeferred.await().getOrDefault(emptyList())
                    .distinct() // géneros duplicados → claves duplicadas en LazyColumn → crash
                    .mapNotNull { genre ->
                        repo.genreTopSongs(genre).getOrNull()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { GenreSection(genre, it.distinctBy { s -> s.id }) }
                    }
                _state.update { it.copy(genres = sections) }
            }
        }
    }

    /** Scroll infinito del carrusel diario: añade canciones aleatorias nuevas (sin repetir ids). */
    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore || s.daily.isEmpty()) return
        if (s.daily.size >= MAX_DAILY) return // tope de memoria/caché del teléfono
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            repo.randomSongs(10)
                .onSuccess { more ->
                    val fresh = more.filter { dailyIds.add(it.id) }
                    _state.update { it.copy(daily = it.daily + fresh, loadingMore = false) }
                }
                .onFailure { _state.update { it.copy(loadingMore = false) } }
        }
    }

    /** Reproduce desde [index] usando [queue] (la lista del carrusel pulsado) como cola. */
    fun play(queue: List<SongDTO>, index: Int) {
        viewModelScope.launch {
            PlaybackConnection.playFrom(queue, index)
        }
    }

    private companion object {
        /** Máximo de canciones acumuladas en el carrusel diario, para acotar memoria e imágenes. */
        const val MAX_DAILY = 120
    }
}
