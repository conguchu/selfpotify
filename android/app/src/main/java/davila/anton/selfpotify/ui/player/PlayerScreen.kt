package davila.anton.selfpotify.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.CoverImage
import davila.anton.selfpotify.ui.theme.Spacing

/**
 * Reproductor a pantalla completa. Se abre desde el mini-player y muestra la carátula en grande,
 * la barra de progreso (con seek), los controles anterior/siguiente, play/pausa, loop y el acceso
 * a "añadir a playlist". El botón superior colapsa de vuelta al contenedor con bottom nav.
 */
@Composable
fun PlayerScreen(
    onCollapse: () -> Unit,
    vm: PlayerViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val inAnyPlaylist by vm.currentInAnyPlaylist.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    // Posición que sigue el dedo mientras se arrastra el slider (null = sigue al player).
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    val positionMs = scrubMs ?: state.positionMs
    val duration = state.durationMs.coerceAtLeast(1)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.player_collapse),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.l))
            CoverImage(
                url = state.artworkUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadius = 12.dp,
            )

            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = state.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = state.artist,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            )

            Spacer(Modifier.height(Spacing.l))
            Slider(
                value = positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                valueRange = 0f..duration.toFloat(),
                onValueChange = { scrubMs = it.toLong() },
                onValueChangeFinished = {
                    scrubMs?.let { vm.seekTo(it) }
                    scrubMs = null
                },
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(positionMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMs(state.durationMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(Spacing.m))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.toggleRepeat() }) {
                    Icon(
                        imageVector = if (state.isLooping) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.player_loop),
                        tint = if (state.isLooping) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { vm.previous() }, enabled = state.hasPrevious) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.player_previous),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp),
                    )
                }
                // Botón central play/pausa, resaltado con el color de acento.
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    IconButton(onClick = { vm.togglePlay() }, modifier = Modifier.size(72.dp)) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.player_pause else R.string.player_play,
                            ),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = { vm.next() }, enabled = state.hasNext) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.player_next),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp),
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
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddToPlaylistSheet(vm = vm, onDismiss = { showAddSheet = false })
    }
}

/** Formatea milisegundos a `m:ss`. */
private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
