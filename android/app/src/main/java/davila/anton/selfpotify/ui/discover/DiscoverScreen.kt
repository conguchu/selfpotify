package davila.anton.selfpotify.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.theme.Spacing
import java.util.Locale

/**
 * Pantalla "Descubrir": columna vertical de secciones, cada una un carrusel horizontal de
 * carátulas (estilo web, sin efecto 3D). Arriba los descubrimientos diarios con scroll infinito,
 * debajo los artistas recomendados y un carrusel por cada género reciente del usuario.
 */
@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues,
    vm: DiscoverViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.loading && state.daily.isEmpty() -> CenterLoader()
            state.error && state.daily.isEmpty() -> CenterMessage(stringResource(R.string.discover_error))
            else -> LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.discover_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            horizontal = Spacing.page,
                            vertical = Spacing.m,
                        ),
                    )
                }

                item {
                    DailyCarousel(
                        songs = state.daily,
                        serverUrl = state.serverUrl,
                        loadingMore = state.loadingMore,
                        onPlay = { index -> vm.play(state.daily, index) },
                        onLoadMore = { vm.loadMore() },
                    )
                }

                if (state.artists.isNotEmpty()) {
                    item {
                        ArtistCarousel(
                            title = stringResource(R.string.discover_artists),
                            artists = state.artists,
                            serverUrl = state.serverUrl,
                        )
                    }
                }

                items(state.genres, key = { it.genre }) { section ->
                    SongCarousel(
                        icon = Icons.Rounded.Album,
                        title = section.genre.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        songs = section.songs,
                        serverUrl = state.serverUrl,
                        onPlay = { index -> vm.play(section.songs, index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterLoader() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
