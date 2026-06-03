package davila.anton.selfpotify.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.repository.DetailRepository
import davila.anton.selfpotify.data.repository.FollowRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla de detalle de usuario (avatar/nombre/contadores + sus playlists). */
data class UserDetailUiState(
    val user: UserSummaryDTO? = null,
    val playlists: List<PlaylistDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    val followLoading: Boolean = false,
)

class UserDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val repo = DetailRepository(session)
    private val follow = FollowRepository(session)

    private val _state = MutableStateFlow(UserDetailUiState())
    val state: StateFlow<UserDetailUiState> = _state.asStateFlow()

    private var loadedId: Long? = null

    fun load(id: Long) {
        if (loadedId == id) return
        loadedId = id
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            _state.update { it.copy(serverUrl = session.current().serverUrl) }
            repo.user(id)
                .onSuccess { (user, playlists) ->
                    _state.update {
                        it.copy(user = user, playlists = playlists.distinctBy { p -> p.id }, loading = false)
                    }
                }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    /** Sigue / deja de seguir al usuario mostrado, según `isFollowedByMe`. */
    fun toggleFollow() {
        val user = _state.value.user ?: return
        if (_state.value.followLoading) return
        _state.update { it.copy(followLoading = true) }
        viewModelScope.launch {
            val result = if (user.isFollowedByMe == true) follow.unfollow(user.id) else follow.follow(user.id)
            result
                .onSuccess { updated -> _state.update { it.copy(user = updated, followLoading = false) } }
                .onFailure { _state.update { it.copy(followLoading = false) } }
        }
    }
}
