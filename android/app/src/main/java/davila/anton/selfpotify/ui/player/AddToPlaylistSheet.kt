package davila.anton.selfpotify.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import davila.anton.selfpotify.ui.common.PlaylistPickerSheet

/**
 * Hoja inferior para añadir o quitar la canción en reproducción de las playlists del usuario
 * (`GET /api/playlists/my` → `POST`/`DELETE /api/playlists/{id}/songs/{songId}`). Delega la UI en
 * [PlaylistPickerSheet]; aquí solo se conecta con el [PlayerViewModel] (canción en curso).
 */
@Composable
fun AddToPlaylistSheet(
    vm: PlayerViewModel,
    onDismiss: () -> Unit,
) {
    val addState by vm.addState.collectAsStateWithLifecycle()
    val player by vm.state.collectAsStateWithLifecycle()
    val songId = player.songId

    LaunchedEffect(Unit) { vm.loadPlaylists() }

    PlaylistPickerSheet(
        loading = addState.loading,
        error = addState.error,
        playlists = addState.playlists,
        isInPlaylist = { songId != null && it.songIds?.contains(songId) == true },
        onToggle = { vm.toggleCurrentInPlaylist(it) },
        onDismiss = onDismiss,
    )
}
