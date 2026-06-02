package davila.anton.selfpotify.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.repository.AuthRepository
import davila.anton.selfpotify.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Destino al que navegar tras una acción del perfil. */
enum class ProfileNav { TO_AUTH, TO_SERVER }

/** Estado de la pantalla Perfil. */
data class ProfileUiState(
    val username: String = "",
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val serverUrl: String? = null,
)

/**
 * ViewModel del perfil. Hereda del antiguo Home las acciones de **cerrar sesión** y **cambiar de
 * servidor** (que ya no viven en una pantalla aparte) y, además, muestra el nombre visible y la
 * foto del usuario (`GET /api/me`).
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
            profile.me().onSuccess { me ->
                _state.update { it.copy(displayName = me.displayName, avatarUrl = me.avatarUrl) }
            }
        }
    }

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
