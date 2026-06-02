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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import davila.anton.selfpotify.ui.common.DetailHeader
import davila.anton.selfpotify.ui.common.DetailTopBar
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/** Pantalla de usuario: avatar + nombre + sus playlists públicas (cada una abre su detalle). */
@Composable
fun UserDetailScreen(
    id: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
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
                        DetailHeader(
                            coverUrl = ServerUrl.asset(state.serverUrl, user.avatarUrl),
                            title = title,
                            subtitle = user.username?.let { "@$it" },
                            circular = true,
                        )
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

/** Fila de playlist en la lista de un usuario: icono + nombre + nº de canciones. */
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
    }
}
