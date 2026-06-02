package davila.anton.selfpotify.ui.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.PlaylistInput
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.repository.DetailRepository
import davila.anton.selfpotify.data.repository.PlaylistRepository
import davila.anton.selfpotify.data.repository.ProfileRepository
import davila.anton.selfpotify.playback.PlaybackConnection
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado del detalle de playlist (metadatos + canciones + modales de edición/compartir). */
data class PlaylistDetailUiState(
    val playlist: PlaylistDTO? = null,
    val songs: List<SongDTO> = emptyList(),
    val serverUrl: String? = null,
    val currentUserId: Long? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    // Modal de edición (solo dueño).
    val editing: Boolean = false,
    val saving: Boolean = false,
    val formError: Boolean = false,
    // Modal de compartir (solo dueño).
    val sharing: Boolean = false,
    val shareLoading: Boolean = false,
    val shareError: Boolean = false,
    val shareUrl: String? = null,
    val collaborators: List<UserSummaryDTO> = emptyList(),
) {
    /** `true` si la playlist es del usuario autenticado (puede editarla/borrarla/compartirla). */
    val isOwner: Boolean
        get() = playlist?.creatorId != null && playlist.creatorId == currentUserId
}

class PlaylistDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)
    private val playlistRepo = PlaylistRepository(session)
    private val profileRepo = ProfileRepository(session)

    private val _state = MutableStateFlow(PlaylistDetailUiState())
    val state: StateFlow<PlaylistDetailUiState> = _state.asStateFlow()

    /** Emite al borrar la playlist para que la pantalla navegue hacia atrás. */
    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted: SharedFlow<Unit> = _deleted.asSharedFlow()

    private var loadedId: Long? = null

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            profileRepo.me().onSuccess { me -> _state.update { it.copy(currentUserId = me.id) } }
            fetch(id)
        }
    }

    private suspend fun fetch(id: Long) {
        repo.playlist(id)
            .onSuccess { (playlist, songs) ->
                _state.update {
                    it.copy(playlist = playlist, songs = songs.distinctBy { s -> s.id }, loading = false)
                }
            }
            .onFailure { _state.update { it.copy(loading = false, error = true) } }
    }

    fun play(index: Int) {
        viewModelScope.launch { PlaybackConnection.playFrom(_state.value.songs, index) }
    }

    /** Quita una canción de la playlist (dueño o colaborador). */
    fun removeSong(songId: Long) {
        val id = loadedId ?: return
        viewModelScope.launch {
            playlistRepo.removeSongFromPlaylist(id, songId).onSuccess { updated ->
                _state.update {
                    it.copy(playlist = updated, songs = it.songs.filterNot { s -> s.id == songId })
                }
            }
        }
    }

    // --- Edición ---

    fun startEdit() = _state.update { it.copy(editing = true, formError = false) }

    fun closeEdit() = _state.update { it.copy(editing = false, saving = false, formError = false) }

    fun savePlaylist(name: String, description: String?, isPublic: Boolean, coverUri: Uri?, removeCover: Boolean = false) {
        val id = loadedId ?: return
        val songIds = _state.value.playlist?.songIds.orEmpty()
        _state.update { it.copy(saving = true, formError = false) }
        viewModelScope.launch {
            val input = PlaylistInput(name = name, description = description, isPublic = isPublic, songIds = songIds)
            playlistRepo.update(id, input)
                .onSuccess {
                    if (coverUri != null) {
                        readBytes(coverUri)?.let { (bytes, mime) ->
                            playlistRepo.uploadCover(id, bytes, mime, "cover")
                        }
                    }
                    _state.update { it.copy(editing = false, saving = false) }
                    fetch(id)
                }
                .onFailure { _state.update { it.copy(saving = false, formError = true) } }
        }
    }

    fun deletePlaylist() {
        val id = loadedId ?: return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            playlistRepo.delete(id)
                .onSuccess { _deleted.tryEmit(Unit) }
                .onFailure { _state.update { it.copy(saving = false, formError = true) } }
        }
    }

    // --- Compartir (magic link) ---

    fun startShare() {
        _state.update { it.copy(sharing = true) }
        generateShareLink()
        loadCollaborators()
    }

    fun closeShare() = _state.update { it.copy(sharing = false) }

    fun generateShareLink() {
        val id = loadedId ?: return
        _state.update { it.copy(shareLoading = true, shareError = false) }
        viewModelScope.launch {
            playlistRepo.shareLink(id)
                .onSuccess { resp ->
                    val path = resp.shareUrl ?: "/api/playlists/share/${resp.token}"
                    _state.update {
                        it.copy(
                            shareLoading = false,
                            shareUrl = ServerUrl.asset(it.serverUrl, path) ?: path,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(shareLoading = false, shareError = true) } }
        }
    }

    private fun loadCollaborators() {
        val id = loadedId ?: return
        viewModelScope.launch {
            playlistRepo.collaborators(id).onSuccess { list ->
                // El creador aparece como colaborador en el backend; no debe poder quitarse a sí mismo.
                val creatorId = _state.value.playlist?.creatorId
                _state.update { it.copy(collaborators = list.filterNot { u -> u.id == creatorId }) }
            }
        }
    }

    fun removeCollaborator(userId: Long) {
        val id = loadedId ?: return
        viewModelScope.launch {
            playlistRepo.removeCollaborator(id, userId).onSuccess {
                loadCollaborators()
                fetch(id)
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
