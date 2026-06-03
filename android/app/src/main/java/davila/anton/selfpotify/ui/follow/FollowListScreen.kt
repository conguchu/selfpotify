package davila.anton.selfpotify.ui.follow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.CenterLoader
import davila.anton.selfpotify.ui.common.CenterMessage
import davila.anton.selfpotify.ui.common.DetailTopBar
import davila.anton.selfpotify.ui.common.UserGridCard
import davila.anton.selfpotify.ui.theme.Spacing

/**
 * Cuadrícula de seguidores o seguidos (estilo escritorio). Pulsar una tarjeta abre ese perfil; en
 * **mi propia** lista de seguidos cada fila lleva un botón *dejar de seguir*.
 */
@Composable
fun FollowListScreen(
    userId: Long,
    type: FollowListType,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    vm: FollowListViewModel = viewModel(),
) {
    LaunchedEffect(userId, type) { vm.load(userId, type) }
    val state by vm.state.collectAsStateWithLifecycle()

    val title = stringResource(
        if (type == FollowListType.FOLLOWERS) R.string.profile_followers else R.string.profile_following,
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            DetailTopBar(title = title, onBack = onBack)
            when {
                state.loading -> CenterLoader()
                state.error -> CenterMessage(stringResource(R.string.detail_error))
                state.users.isEmpty() -> CenterMessage(stringResource(R.string.follow_list_empty))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(
                        start = Spacing.s,
                        end = Spacing.s,
                        top = Spacing.s,
                        bottom = contentPadding.calculateBottomPadding() + Spacing.s,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    items(state.users, key = { it.id }) { user ->
                        UserGridCard(
                            user = user,
                            serverUrl = state.serverUrl,
                            onClick = { onOpenUser(user.id) },
                            trailing = if (state.showUnfollow) {
                                {
                                    OutlinedButton(onClick = { vm.unfollow(user.id) }) {
                                        Text(stringResource(R.string.profile_unfollow))
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}
