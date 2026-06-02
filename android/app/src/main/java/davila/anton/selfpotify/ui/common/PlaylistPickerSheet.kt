package davila.anton.selfpotify.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.ui.theme.Spacing

/**
 * Hoja inferior reutilizable para añadir/quitar una canción de las playlists del usuario. No conoce
 * de dónde sale la canción (reproductor, detalle de artista, etc.): recibe el estado ya resuelto y
 * dos lambdas. Cada fila muestra un check según [isInPlaylist]; pulsarla invoca [onToggle].
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaylistPickerSheet(
    loading: Boolean,
    error: Boolean,
    playlists: List<PlaylistDTO>,
    isInPlaylist: (PlaylistDTO) -> Boolean,
    onToggle: (PlaylistDTO) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.add_to_playlist_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.s),
        )
        when {
            loading -> Box(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                Alignment.Center,
            ) { CircularProgressIndicator() }

            error -> Message(stringResource(R.string.add_to_playlist_error))

            playlists.isEmpty() -> Message(stringResource(R.string.add_to_playlist_empty))

            else -> LazyColumn(modifier = Modifier.padding(bottom = Spacing.l)) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(playlist, isInPlaylist(playlist)) { onToggle(playlist) }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: PlaylistDTO, inPlaylist: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
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
        Column(Modifier.weight(1f)) {
            Text(
                text = playlist.name.orEmpty(),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.playlist_song_count, playlist.songIds?.size ?: 0),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (inPlaylist) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = stringResource(
                if (inPlaylist) R.string.add_to_playlist_remove else R.string.add_to_playlist_add,
            ),
            tint = if (inPlaylist) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxWidth().padding(Spacing.xl), Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
