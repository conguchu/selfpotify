package davila.anton.selfpotify.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.ui.common.CenterLoader
import davila.anton.selfpotify.ui.common.CenterMessage
import davila.anton.selfpotify.ui.common.DetailTopBar
import davila.anton.selfpotify.ui.common.FollowCountsRow
import davila.anton.selfpotify.ui.common.ProfileAvatar
import davila.anton.selfpotify.ui.common.ProfileNameRow
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/**
 * Perfil de otro usuario: reutiliza los componentes del perfil propio (avatar + nombre +
 * contadores) **sin** iconos de edición, añade el botón **seguir / dejar de seguir** y lista sus
 * playlists públicas o colaborativas conmigo (icono de personitas en las colaborativas).
 */
@Composable
fun UserDetailScreen(
    id: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFollowers: (Long) -> Unit,
    onOpenFollowing: (Long) -> Unit,
    vm: UserDetailViewModel = viewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            val user = state.user
            val title = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username.orEmpty()
            DetailTopBar(title = title, onBack = onBack)
            when {
                state.loading -> CenterLoader()
                state.error || user == null -> CenterMessage(stringResource(R.string.detail_error))
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.page, vertical = Spacing.m),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.m),
                        ) {
                            ProfileAvatar(
                                url = ServerUrl.asset(state.serverUrl, user.avatarUrl),
                                editable = false,
                            )
                            ProfileNameRow(displayName = user.displayName, username = user.username)
                            FollowCountsRow(
                                followers = user.followersCount,
                                following = user.followingCount,
                                onFollowersClick = { onOpenFollowers(user.id) },
                                onFollowingClick = { onOpenFollowing(user.id) },
                            )
                            FollowButton(
                                following = user.isFollowedByMe == true,
                                loading = state.followLoading,
                                onClick = vm::toggleFollow,
                            )
                        }
                    }
                    if (state.playlists.isEmpty()) {
                        item { CenterMessage(stringResource(R.string.detail_no_playlists)) }
                    } else {
                        items(state.playlists, key = { it.id }) { playlist ->
                            PlaylistRow(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                        }
                    }
                }
            }
        }
    }
}

/** Botón seguir/siguiendo: relleno cuando aún no sigo, contorno cuando ya sigo. */
@Composable
private fun FollowButton(following: Boolean, loading: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val label = stringResource(
        if (following) R.string.profile_following_state else R.string.profile_follow,
    )
    val content: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Text(label)
        }
    }
    if (following) {
        OutlinedButton(onClick = onClick, enabled = !loading, shape = shape) { content() }
    } else {
        Button(onClick = onClick, enabled = !loading, shape = shape) { content() }
    }
}

/** Fila de playlist: icono + nombre + nº de canciones; icono de personitas si es colaborativa. */
@Composable
private fun PlaylistRow(playlist: PlaylistDTO, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.page, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = playlist.name.orEmpty(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.playlist_song_count, playlist.songIds?.size ?: 0),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (playlist.isShared) {
            Icon(
                imageVector = Icons.Rounded.People,
                contentDescription = stringResource(R.string.cd_playlist_shared),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
