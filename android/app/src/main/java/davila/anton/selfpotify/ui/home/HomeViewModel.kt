package davila.anton.selfpotify.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Destino al que navegar tras una acción del home. */
enum class HomeNav { TO_AUTH, TO_SERVER }

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = AuthRepository(session)

    val username: StateFlow<String?> = session.session
        .map { it.username }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _navigate = MutableSharedFlow<HomeNav>(extraBufferCapacity = 1)
    val navigate: SharedFlow<HomeNav> = _navigate.asSharedFlow()

    /** Logout: borra el JWT (conserva el servidor) y vuelve al login. */
    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _navigate.emit(HomeNav.TO_AUTH)
        }
    }

    /** Cambiar de servidor: borra servidor + JWT y vuelve a la pantalla de servidor. */
    fun changeServer() {
        viewModelScope.launch {
            repo.forgetServer()
            _navigate.emit(HomeNav.TO_SERVER)
        }
    }
}
