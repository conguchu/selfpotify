package davila.anton.selfpotify.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Modo de la pantalla: iniciar sesión o crear cuenta. */
enum class AuthMode { LOGIN, REGISTER }

enum class AuthError { INVALID_CREDENTIALS, USERNAME_TAKEN, EMPTY_FIELDS, NETWORK, UNKNOWN }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val loading: Boolean = false,
    val error: AuthError? = null,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(SessionStore(app))

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    private val _navigateToServer = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToServer: SharedFlow<Unit> = _navigateToServer.asSharedFlow()

    fun toggleMode() {
        _state.update {
            val next = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
            it.copy(mode = next, error = null)
        }
    }

    fun clearError() {
        if (_state.value.error != null) _state.update { it.copy(error = null) }
    }

    /** Cambiar de servidor: borra servidor + JWT y vuelve a la pantalla de servidor. */
    fun changeServer() {
        viewModelScope.launch {
            repo.forgetServer()
            _navigateToServer.emit(Unit)
        }
    }

    fun submit(username: String, password: String) {
        if (_state.value.loading) return
        val user = username.trim()
        if (user.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(error = AuthError.EMPTY_FIELDS) }
            return
        }

        val mode = _state.value.mode
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = when (mode) {
                AuthMode.LOGIN -> repo.login(user, password)
                AuthMode.REGISTER -> repo.register(user, password)
            }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(loading = false) }
                    _navigateToHome.emit(Unit)
                },
                onFailure = { t ->
                    _state.update { it.copy(loading = false, error = mapError(mode, t)) }
                },
            )
        }
    }

    private fun mapError(mode: AuthMode, t: Throwable): AuthError = when {
        t is HttpException && t.code() == 401 -> AuthError.INVALID_CREDENTIALS
        t is HttpException && t.code() == 400 && mode == AuthMode.REGISTER -> AuthError.USERNAME_TAKEN
        t is IOException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }
}
