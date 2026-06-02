package davila.anton.selfpotify.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.CenterLoader
import davila.anton.selfpotify.ui.common.CenterMessage
import davila.anton.selfpotify.ui.common.DetailHeader
import davila.anton.selfpotify.ui.common.DetailTopBar
import davila.anton.selfpotify.ui.common.SongRow

/** Pantalla de playlist: nombre/descripción + sus canciones (reproducibles). */
@Composable
fun PlaylistDetailScreen(
    id: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    vm: PlaylistDetailViewModel = viewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            DetailTopBar(title = state.playlist?.name.orEmpty(), onBack = onBack)
            when {
                state.loading -> CenterLoader()
                state.error || state.playlist == null -> CenterMessage(stringResource(R.string.detail_error))
                else -> {
                    val playlist = state.playlist!!
                    val subtitle = playlist.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.playlist_song_count, state.songs.size)
                    LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                        item {
                            // PlaylistDTO de la app no expone carátula: header con placeholder.
                            DetailHeader(
                                coverUrl = null,
                                title = playlist.name.orEmpty(),
                                subtitle = subtitle,
                                circular = false,
                            )
                        }
                        if (state.songs.isEmpty()) {
                            item { CenterMessage(stringResource(R.string.detail_no_tracks)) }
                        } else {
                            itemsIndexed(state.songs, key = { _, s -> s.id }) { index, song ->
                                SongRow(
                                    song = song,
                                    serverUrl = state.serverUrl,
                                    onClick = { vm.play(index) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
