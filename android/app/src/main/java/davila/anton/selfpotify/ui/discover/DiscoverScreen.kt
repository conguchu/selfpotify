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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    contentPadding: PaddingValues,
    onOpenArtist: (Long) -> Unit,
    vm: DiscoverViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Cada refresco incrementa este contador; al usarlo como `key` de la lista se recrea su
    // subárbol y todas las lazy lists (la columna y los carruseles) vuelven a su primer elemento,
    // para que el contenido recargado no quede a media posición de scroll.
    var refreshGen by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.refreshing) {
        if (state.refreshing) refreshGen++
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.loading && state.daily.isEmpty() -> CenterLoader()
            state.error && state.daily.isEmpty() -> CenterMessage(stringResource(R.string.discover_error))
            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                key(refreshGen) {
                    LazyColumn(
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
                                onPlay = { index -> vm.play(state.daily, index, extendable = true) },
                                onLoadMore = { vm.loadMore() },
                            )
                        }

                        if (state.artists.isNotEmpty()) {
                            item {
                                ArtistCarousel(
                                    title = stringResource(R.string.discover_artists),
                                    artists = state.artists,
                                    serverUrl = state.serverUrl,
                                    onArtistClick = onOpenArtist,
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
