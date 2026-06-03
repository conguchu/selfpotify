package davila.anton.selfpotify.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.repository.AuthRepository
import davila.anton.selfpotify.data.repository.ProfileRepository
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

/** Destino al que navegar tras una acción del perfil. */
enum class ProfileNav { TO_AUTH, TO_SERVER }

/** Estado de la pantalla Perfil propio. */
data class ProfileUiState(
    val id: Long? = null,
    val username: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val serverUrl: String? = null,
    val refreshing: Boolean = false,
    val savingName: Boolean = false,
    val uploadingPhoto: Boolean = false,
    val showNameDialog: Boolean = false,
    val showPhotoSheet: Boolean = false,
)

/**
 * ViewModel del perfil propio. Hereda del antiguo Home las acciones de **cerrar sesión** y
 * **cambiar de servidor**, y añade la **edición inline** del nombre visible (`PUT /api/me/profile`)
 * y de la foto (`POST`/`DELETE /api/me/profile/picture`), además de los contadores de
 * seguidores/seguidos (`GET /api/me`).
 */
class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val auth = AuthRepository(session)
    private val profile = ProfileRepository(session)

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _navigate = MutableSharedFlow<ProfileNav>(extraBufferCapacity = 1)
    val navigate: SharedFlow<ProfileNav> = _navigate.asSharedFlow()

    init {
        viewModelScope.launch {
            val current = session.current()
            _state.update { it.copy(username = current.username.orEmpty(), serverUrl = current.serverUrl) }
            refresh()
        }
    }

    /** Recarga el perfil (`GET /api/me`). Activa el indicador de *pull-to-refresh*. */
    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            profile.me().onSuccess { applyUser(it) }
            _state.update { it.copy(refreshing = false) }
        }
    }

    private fun applyUser(me: UserSummaryDTO) {
        _state.update {
            it.copy(
                id = me.id,
                username = me.username ?: it.username,
                displayName = me.displayName,
                avatarUrl = me.avatarUrl,
                followersCount = me.followersCount,
                followingCount = me.followingCount,
            )
        }
    }

    // --- Edición del nombre ---

    fun openNameDialog() = _state.update { it.copy(showNameDialog = true) }

    fun closeNameDialog() = _state.update { it.copy(showNameDialog = false, savingName = false) }

    /** Guarda el nombre visible (vacío lo borra). Cierra el diálogo al terminar. */
    fun saveName(name: String) {
        _state.update { it.copy(savingName = true) }
        viewModelScope.launch {
            profile.updateName(name)
                .onSuccess {
                    applyUser(it)
                    _state.update { s -> s.copy(showNameDialog = false, savingName = false) }
                }
                .onFailure { _state.update { s -> s.copy(savingName = false) } }
        }
    }

    // --- Edición de la foto ---

    fun openPhotoSheet() = _state.update { it.copy(showPhotoSheet = true) }

    fun closePhotoSheet() = _state.update { it.copy(showPhotoSheet = false) }

    /** Sube la foto elegida en el selector. Lee los bytes del `Uri` con `ContentResolver`. */
    fun changePhoto(uri: Uri) {
        _state.update { it.copy(showPhotoSheet = false, uploadingPhoto = true) }
        viewModelScope.launch {
            val read = readBytes(uri)
            if (read == null) {
                _state.update { it.copy(uploadingPhoto = false) }
                return@launch
            }
            val (bytes, mime) = read
            profile.uploadAvatar(bytes, mime, "avatar")
                .onSuccess {
                    applyUser(it)
                    _state.update { s -> s.copy(uploadingPhoto = false) }
                }
                .onFailure { _state.update { s -> s.copy(uploadingPhoto = false) } }
        }
    }

    /** Borra la foto de perfil. */
    fun removePhoto() {
        _state.update { it.copy(showPhotoSheet = false, uploadingPhoto = true) }
        viewModelScope.launch {
            profile.deleteAvatar()
                .onSuccess {
                    applyUser(it)
                    _state.update { s -> s.copy(uploadingPhoto = false) }
                }
                .onFailure { _state.update { s -> s.copy(uploadingPhoto = false) } }
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

    // --- Sesión / servidor ---

    /** Logout: borra el JWT (conserva el servidor) y vuelve al login. */
    fun logout() {
        viewModelScope.launch {
            auth.logout()
            _navigate.emit(ProfileNav.TO_AUTH)
        }
    }

    /** Cambiar de servidor: borra servidor + JWT + marca y vuelve a la pantalla de servidor. */
    fun changeServer() {
        viewModelScope.launch {
            auth.forgetServer()
            _navigate.emit(ProfileNav.TO_SERVER)
        }
    }
}
