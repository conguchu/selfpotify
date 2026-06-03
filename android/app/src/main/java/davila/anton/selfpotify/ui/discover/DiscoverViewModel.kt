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
    val refreshing: Boolean = false,
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

    /**
     * Reproducir desde el carrusel diario hace que su cola se **autoextienda**: al llegar a la
     * última canción se piden más aleatorias y se añaden al final, para que la reproducción no se
     * detenga. Estos campos rastrean la cola del player (independiente del carrusel visible):
     * [dailyQueueActive] indica si la cola en curso es la diaria, [queueIds] sus ids (para no
     * repetir) y [lastExtendedId] la última canción para la que ya se pidió extensión (evita
     * pedir lo mismo en cada tick de posición del player).
     */
    private var dailyQueueActive = false
    private val queueIds = mutableSetOf<Long>()
    private var lastExtendedId: Long? = null
    private var extending = false

    init {
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
        }
        viewModelScope.launch {
            PlaybackConnection.state.collect { player ->
                // En la última canción de la cola diaria, el player ya no tiene "siguiente":
                // pedimos más y las añadimos. Una sola vez por canción (lastExtendedId). El
                // `songId in queueIds` evita extender una cola ajena si otra pantalla toma el player.
                if (dailyQueueActive && player.hasItem && !player.hasNext &&
                    player.songId != null && player.songId in queueIds &&
                    player.songId != lastExtendedId
                ) {
                    lastExtendedId = player.songId
                    extendQueue()
                }
            }
        }
        load()
    }

    /** Carga inicial: descubrimientos diarios, artistas recomendados y carruseles por género. */
    fun load() {
        if (_state.value.loading || _state.value.refreshing) return
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch { fetch() }
    }

    /**
     * Pull-to-refresh: vuelve a pedir todas las secciones manteniendo el contenido actual visible
     * mientras llega el nuevo (a diferencia de [load], que muestra el loader a pantalla completa).
     */
    fun refresh() {
        if (_state.value.loading || _state.value.refreshing) return
        _state.update { it.copy(refreshing = true, error = false) }
        viewModelScope.launch { fetch() }
    }

    /** Lógica compartida por carga inicial y refresco: descubrimientos, artistas y géneros. */
    private suspend fun fetch() {
        // Daily y artistas en paralelo; los géneros dependen de su lista, así que van después.
        val dailyDeferred = viewModelScope.async { repo.dailyDiscoveries() }
        val artistsDeferred = viewModelScope.async { repo.homeFeed() }
        val genreNamesDeferred = viewModelScope.async { repo.recentGenres() }

        val daily = dailyDeferred.await()
        val artists = artistsDeferred.await().getOrDefault(emptyList())

        daily.onSuccess { songs ->
            // distinctBy para blindar el LazyRow ante IDs duplicados que pueda devolver la API.
            val unique = songs.distinctBy { it.id }
            dailyIds.clear()
            dailyIds.addAll(unique.map { it.id })
            _state.update {
                it.copy(daily = unique, artists = artists, loading = false, refreshing = false)
            }
        }.onFailure {
            // En refresco se conserva el contenido previo; solo se marca error si no había nada.
            _state.update {
                it.copy(loading = false, refreshing = false, error = it.daily.isEmpty())
            }
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

    /**
     * Reproduce desde [index] usando [queue] (la lista del carrusel pulsado) como cola. Si
     * [extendable] es `true` (carrusel diario), la cola se autoextiende al llegar al final.
     */
    fun play(queue: List<SongDTO>, index: Int, extendable: Boolean = false) {
        dailyQueueActive = extendable
        lastExtendedId = null
        queueIds.clear()
        if (extendable) queueIds.addAll(queue.map { it.id })
        viewModelScope.launch {
            PlaybackConnection.playFrom(queue, index)
        }
    }

    /**
     * Pide más canciones aleatorias y las añade al final de la cola del player. Reintenta unas
     * pocas veces si solo llegan ids ya en cola, para no quedarse sin extender por duplicados.
     */
    private fun extendQueue() {
        if (extending) return
        extending = true
        viewModelScope.launch {
            try {
                repeat(EXTEND_RETRIES) {
                    val more = repo.randomSongs(EXTEND_BATCH).getOrDefault(emptyList())
                    val fresh = more.filter { queueIds.add(it.id) }
                    if (fresh.isNotEmpty()) {
                        PlaybackConnection.appendSongs(fresh)
                        return@launch
                    }
                }
            } finally {
                extending = false
            }
        }
    }

    private companion object {
        /** Máximo de canciones acumuladas en el carrusel diario, para acotar memoria e imágenes. */
        const val MAX_DAILY = 120

        /** Canciones que se piden cada vez que se autoextiende la cola de reproducción. */
        const val EXTEND_BATCH = 10

        /** Intentos para conseguir canciones nuevas (no repetidas) al extender la cola. */
        const val EXTEND_RETRIES = 3
    }
}
