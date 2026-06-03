package davila.anton.selfpotify.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.ui.common.CoverImage
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

private val CARD_WIDTH = 140.dp
private val ARTIST_WIDTH = 120.dp

/** Cabecera de sección: icono de acento + título. */
@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.page),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Carrusel de descubrimientos diarios con scroll infinito: al acercarse al final dispara
 * [onLoadMore] y, mientras carga el siguiente lote, muestra una tarjeta con spinner.
 */
@Composable
fun DailyCarousel(
    songs: List<SongDTO>,
    serverUrl: String?,
    loadingMore: Boolean,
    onPlay: (Int) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 2
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.Rounded.AutoAwesome, stringResource(R.string.discover_daily))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongCard(song = song, serverUrl = serverUrl, onClick = { onPlay(index) })
            }
            if (loadingMore) {
                item { LoaderCard() }
            }
        }
    }
}

/** Carrusel de canciones (top de un género). Pulsar una canción reproduce desde su posición. */
@Composable
fun SongCarousel(
    icon: ImageVector,
    title: String,
    songs: List<SongDTO>,
    serverUrl: String?,
    onPlay: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(icon, title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                SongCard(song = song, serverUrl = serverUrl, onClick = { onPlay(index) })
            }
        }
    }
}

/** Carrusel de artistas (foto circular + nombre). Pulsar un artista abre su pantalla de detalle. */
@Composable
fun ArtistCarousel(
    title: String,
    artists: List<ArtistDTO>,
    serverUrl: String?,
    onArtistClick: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        SectionHeader(Icons.Rounded.Group, title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.page),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            items(artists, key = { it.id }) { artist ->
                ArtistCard(artist = artist, serverUrl = serverUrl, onClick = { onArtistClick(artist.id) })
            }
        }
    }
}

@Composable
private fun SongCard(song: SongDTO, serverUrl: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(CARD_WIDTH)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        CoverImage(
            url = ServerUrl.asset(serverUrl, song.pictureUrl),
            modifier = Modifier.size(CARD_WIDTH),
        )
        Text(
            text = song.title.orEmpty(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artistsLabel,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistCard(artist: ArtistDTO, serverUrl: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(ARTIST_WIDTH)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        ArtistAvatar(
            url = ServerUrl.asset(serverUrl, artist.photoUrl),
            modifier = Modifier.size(ARTIST_WIDTH),
        )
        Text(
            text = artist.name.orEmpty(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Foto circular del artista; mientras carga o si falta/falla pinta un icono de persona. */
@Composable
private fun ArtistAvatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            PersonPlaceholder()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { PersonPlaceholder() },
                error = { PersonPlaceholder() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PersonPlaceholder() {
    Icon(
        imageVector = Icons.Rounded.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(40.dp),
    )
}

@Composable
private fun LoaderCard() {
    Box(modifier = Modifier.size(CARD_WIDTH), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
