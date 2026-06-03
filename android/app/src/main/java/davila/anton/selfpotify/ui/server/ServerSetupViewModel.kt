package davila.anton.selfpotify.ui.server

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.repository.AuthRepository
import davila.anton.selfpotify.data.repository.NotSelfpotifyServerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la validación de la dirección del servidor. */
sealed interface ServerUiState {
    data object Idle : ServerUiState
    data object Validating : ServerUiState
    data class Valid(val appName: String) : ServerUiState
    data class Invalid(val error: ServerError) : ServerUiState
}

enum class ServerError { UNREACHABLE, NOT_SELFPOTIFY }

class ServerSetupViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(SessionStore(app))

    private val _state = MutableStateFlow<ServerUiState>(ServerUiState.Idle)
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    private val _navigateToAuth = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToAuth: SharedFlow<Unit> = _navigateToAuth.asSharedFlow()

    private var address: String = ""
    private var validateJob: Job? = null
    private var validatedColors: Map<String, String>? = null
    private var validatedLogoUrl: String? = null

    /** Se llama mientras el usuario escribe; valida con debounce cuando deja de escribir. */
    fun onAddressChanged(raw: String) {
        address = raw
        validateJob?.cancel()
        _state.value = ServerUiState.Idle // desactiva "Siguiente" mientras edita
        if (raw.isBlank()) return

        validateJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _state.value = ServerUiState.Validating
            validatedColors = null
            validatedLogoUrl = null
            val result = repo.validateServer(raw)
            if (address != raw) return@launch // el texto cambió: descartar resultado obsoleto
            _state.value = result.fold(
                onSuccess = {
                    validatedColors = it.branding?.colors
                    validatedLogoUrl = it.branding?.logoUrl
                    ServerUiState.Valid(it.branding?.appName.orEmpty())
                },
                onFailure = {
                    val error = if (it is NotSelfpotifyServerException) {
                        ServerError.NOT_SELFPOTIFY
                    } else {
                        ServerError.UNREACHABLE
                    }
                    ServerUiState.Invalid(error)
                },
            )
        }
    }

    /** Persiste el servidor validado y navega al login. */
    fun onNextClicked() {
        if (_state.value !is ServerUiState.Valid) return
        viewModelScope.launch {
            repo.saveServer(address)
            // Adopta la marca del servidor (paleta + logo) ya desde el login, antes de autenticarse.
            repo.saveBranding(validatedColors, validatedLogoUrl)
            _navigateToAuth.emit(Unit)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 600L
    }
}
