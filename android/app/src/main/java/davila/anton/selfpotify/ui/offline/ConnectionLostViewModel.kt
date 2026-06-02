package davila.anton.selfpotify.ui.offline

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
import kotlinx.coroutines.launch

/** Destino al que navegar desde la pantalla de sin-conexión. */
enum class OfflineNav { TO_HOME, TO_SERVER }

/**
 * Pantalla de sin-conexión: se muestra cuando el usuario está logueado pero el servidor
 * no responde. Ofrece reintentar la conexión o desconectarse del servidor.
 */
class ConnectionLostViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = AuthRepository(session)

    private val _retrying = MutableStateFlow(false)
    val retrying: StateFlow<Boolean> = _retrying.asStateFlow()

    private val _navigate = MutableSharedFlow<OfflineNav>(extraBufferCapacity = 1)
    val navigate: SharedFlow<OfflineNav> = _navigate.asSharedFlow()

    /** Reintenta la conexión; si el servidor vuelve a responder, navega al home. */
    fun retry() {
        if (_retrying.value) return
        viewModelScope.launch {
            _retrying.value = true
            val ok = repo.checkConnection()
            _retrying.value = false
            if (ok) _navigate.emit(OfflineNav.TO_HOME)
        }
    }

    /** Desconectarse del servidor: borra servidor + JWT + paleta y vuelve a la pantalla 1. */
    fun disconnect() {
        viewModelScope.launch {
            repo.forgetServer()
            _navigate.emit(OfflineNav.TO_SERVER)
        }
    }
}
