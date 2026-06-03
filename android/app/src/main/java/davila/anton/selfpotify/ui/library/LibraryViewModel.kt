package davila.anton.selfpotify.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.PlaylistInput
import davila.anton.selfpotify.data.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado de la pantalla Biblioteca: playlists propias + compartidas y el modal de alta. */
data class LibraryUiState(
    val myPlaylists: List<PlaylistDTO> = emptyList(),
    val sharedPlaylists: List<PlaylistDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val creating: Boolean = false,
    val saving: Boolean = false,
    val formError: Boolean = false,
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = PlaylistRepository(session)

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    /** Emite el id de la playlist recién creada para navegar a su detalle. */
    private val _created = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val created: SharedFlow<Long> = _created.asSharedFlow()

    fun load() {
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            val mine = async { repo.myPlaylists() }
            val shared = async { repo.sharedPlaylists() }
            val mineResult = mine.await()
            val sharedResult = shared.await()
            mineResult.onSuccess { my ->
                _state.update {
                    it.copy(
                        myPlaylists = my,
                        sharedPlaylists = sharedResult.getOrDefault(emptyList()),
                        loading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(loading = false, error = true) }
            }
        }
    }

    fun openCreate() = _state.update { it.copy(creating = true, formError = false) }

    fun closeCreate() = _state.update { it.copy(creating = false, saving = false, formError = false) }

    /** Crea una playlist (con carátula opcional). Al terminar refresca y navega a su detalle. */
    fun createPlaylist(name: String, description: String?, isPublic: Boolean, coverUri: Uri?) {
        _state.update { it.copy(saving = true, formError = false) }
        viewModelScope.launch {
            val input = PlaylistInput(name = name, description = description, isPublic = isPublic, songIds = emptyList())
            repo.create(input)
                .onSuccess { playlist ->
                    if (coverUri != null) {
                        readBytes(coverUri)?.let { (bytes, mime) ->
                            repo.uploadCover(playlist.id, bytes, mime, "cover")
                        }
                    }
                    _state.update { it.copy(creating = false, saving = false) }
                    _created.tryEmit(playlist.id)
                    load()
                }
                .onFailure {
                    _state.update { it.copy(saving = false, formError = true) }
                }
        }
    }

    private suspend fun readBytes(uri: Uri): Pair<ByteArray, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = getApplication<Application>().contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            bytes to mime
        }.getOrNull()
    }
}
