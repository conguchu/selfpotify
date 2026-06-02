package davila.anton.selfpotify.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.CoverImage
import davila.anton.selfpotify.ui.theme.Spacing

/**
 * Mini-player persistente sobre la barra de navegación (estilo Spotify, CLAUDE.md §3.2).
 * Solo se muestra cuando hay una canción cargada. Pulsar el cuerpo expande el reproductor
 * completo; incluye play/pausa y un acceso a "añadir a playlist".
 */
@Composable
fun MiniPlayer(
    vm: PlayerViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.hasItem) return

    val inAnyPlaylist by vm.currentInAnyPlaylist.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onExpand)
                .fillMaxWidth()
                .padding(horizontal = Spacing.s, vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            CoverImage(url = state.artworkUrl, modifier = Modifier.size(48.dp), cornerRadius = 4.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { showAddSheet = true }) {
                Icon(
                    imageVector = if (inAnyPlaylist) Icons.AutoMirrored.Rounded.PlaylistAddCheck
                    else Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = stringResource(
                        if (inAnyPlaylist) R.string.player_in_playlist else R.string.player_add_to_playlist,
                    ),
                    tint = if (inAnyPlaylist) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = { vm.togglePlay() }) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.player_pause else R.string.player_play,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    if (showAddSheet) {
        AddToPlaylistSheet(vm = vm, onDismiss = { showAddSheet = false })
    }
}
