package davila.anton.selfpotify.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.repository.AuthRepository
import davila.anton.selfpotify.data.repository.PlaylistRepository
import davila.anton.selfpotify.data.repository.SessionStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del contenedor principal. Al entrar verifica la sesión contra el servidor:
 * - si el servidor no responde, navega a la pantalla de sin-conexión;
 * - si el servidor rechaza el JWT (expirado o inválido), cierra sesión y vuelve al login.
 * (README, sección Android → flujo de acceso y pantalla de sin-conexión).
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(SessionStore(app))
    private val playlists = PlaylistRepository(SessionStore(app))

    private val _navigateOffline = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateOffline: SharedFlow<Unit> = _navigateOffline.asSharedFlow()

    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    /** Id de la playlist a la que navegar tras canjear un magic link con éxito. */
    private val _openPlaylist = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val openPlaylist: SharedFlow<Long> = _openPlaylist.asSharedFlow()

    /** El canje del magic link falló (token inválido, ya usado, propia playlist…). */
    private val _shareError = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shareError: SharedFlow<Unit> = _shareError.asSharedFlow()

    /**
     * Canjea el [token] del deep link contra el servidor configurado. En éxito emite el id de la
     * playlist para navegar a su detalle; en error emite [shareError] para avisar al usuario.
     */
    fun redeemShare(token: String) {
        viewModelScope.launch {
            playlists.redeem(token)
                .onSuccess { _openPlaylist.emit(it.id) }
                .onFailure { _shareError.emit(Unit) }
        }
    }

    fun checkSession() {
        viewModelScope.launch {
            when (repo.verifySession()) {
                SessionStatus.OFFLINE -> _navigateOffline.emit(Unit)
                SessionStatus.EXPIRED -> {
                    repo.logout()
                    _sessionExpired.emit(Unit)
                }
                SessionStatus.VALID -> Unit
            }
        }
    }
}
