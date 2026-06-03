package davila.anton.selfpotify.ui.follow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.data.repository.FollowRepository
import davila.anton.selfpotify.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Qué lista se está mostrando. */
enum class FollowListType { FOLLOWERS, FOLLOWING }

/** Estado de la cuadrícula de seguidores/seguidos. */
data class FollowListUiState(
    val users: List<UserSummaryDTO> = emptyList(),
    val serverUrl: String? = null,
    val loading: Boolean = true,
    val error: Boolean = false,
    /** El botón *dejar de seguir* por fila solo se muestra en **mi propia** lista de seguidos. */
    val showUnfollow: Boolean = false,
)

class FollowListViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)
    private val follow = FollowRepository(session)
    private val profile = ProfileRepository(session)

    private val _state = MutableStateFlow(FollowListUiState())
    val state: StateFlow<FollowListUiState> = _state.asStateFlow()

    private var loaded: Pair<Long, FollowListType>? = null

    fun load(userId: Long, type: FollowListType) {
        if (loaded == userId to type) return
        loaded = userId to type
        _state.update { it.copy(loading = true, error = false) }
        viewModelScope.launch {
            val myId = profile.me().getOrNull()?.id
            val result = if (type == FollowListType.FOLLOWERS) follow.followers(userId) else follow.following(userId)
            result
                .onSuccess { users ->
                    _state.update {
                        it.copy(
                            users = users,
                            serverUrl = session.current().serverUrl,
                            loading = false,
                            showUnfollow = type == FollowListType.FOLLOWING && userId == myId,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(loading = false, error = true) } }
        }
    }

    /** Deja de seguir a [targetId] y lo retira de la lista (solo en mi lista de seguidos). */
    fun unfollow(targetId: Long) {
        viewModelScope.launch {
            follow.unfollow(targetId).onSuccess {
                _state.update { s -> s.copy(users = s.users.filterNot { it.id == targetId }) }
            }
        }
    }
}
