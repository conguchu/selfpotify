package davila.anton.selfpotify.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/** Barra superior mínima de una pantalla de detalle: flecha de retroceso + título. */
@Composable
fun DetailTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResourceBack(),
            )
        }
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

@Composable
private fun stringResourceBack(): String =
    androidx.compose.ui.res.stringResource(R.string.cd_back)

/**
 * Cabecera de una pantalla de detalle: carátula grande (cuadrada o circular), título y subtítulo
 * centrados. [circular] se usa para fotos de artista/usuario; el resto van cuadradas.
 */
@Composable
fun DetailHeader(
    coverUrl: String?,
    title: String,
    subtitle: String?,
    circular: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.page, vertical = Spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        val coverModifier = Modifier.size(180.dp)
        if (circular) {
            CircleAvatar(url = coverUrl, modifier = coverModifier)
        } else {
            CoverImage(url = coverUrl, modifier = coverModifier, cornerRadius = 12.dp)
        }
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Fila de canción en una lista vertical: carátula pequeña + título + artistas. Pulsar reproduce.
 *
 * Opcionalmente, al estilo de la vista web (`SongRow.tsx`):
 * - [position] pinta el número de orden a la izquierda (1, 2, 3…).
 * - [listeners], si no es `null`, muestra el contador de escuchas (icono ▶ + nº) a la derecha.
 * - [onAddToPlaylist], si no es `null`, añade un botón "+" para meter la canción en una playlist.
 * - [onRemoveFromPlaylist], si no es `null`, añade un botón "-" para quitar la canción de la playlist
 *   actual (usado en el detalle de playlist).
 */
@Composable
fun SongRow(
    song: SongDTO,
    serverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: Int? = null,
    listeners: Long? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = Spacing.page, end = Spacing.s, top = Spacing.s, bottom = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        if (position != null) {
            Text(
                text = position.toString(),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 20.dp),
            )
        }
        CoverImage(
            url = ServerUrl.asset(serverUrl, song.pictureUrl),
            modifier = Modifier.size(48.dp),
            cornerRadius = 6.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title.orEmpty(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artistsLabel,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (listeners != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = stringResourcePlays(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = listeners.toString(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResourceAddToPlaylist(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onRemoveFromPlaylist != null) {
            IconButton(onClick = onRemoveFromPlaylist) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = stringResourceRemoveFromPlaylist(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun stringResourceRemoveFromPlaylist(): String =
    androidx.compose.ui.res.stringResource(R.string.cd_remove_from_playlist)

@Composable
private fun stringResourcePlays(): String =
    androidx.compose.ui.res.stringResource(R.string.cd_play_count)

@Composable
private fun stringResourceAddToPlaylist(): String =
    androidx.compose.ui.res.stringResource(R.string.player_add_to_playlist)

/** Foto circular (artista/usuario); placeholder de persona mientras carga o si falta/falla. */
@Composable
fun CircleAvatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            PersonIcon()
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { PersonIcon() },
                error = { PersonIcon() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PersonIcon() {
    Icon(
        imageVector = Icons.Rounded.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(56.dp),
    )
}

/** Spinner centrado en toda la pantalla. */
@Composable
fun CenterLoader() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
}

/** Mensaje centrado en toda la pantalla (error / vacío). */
@Composable
fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(Spacing.xl), Alignment.Center) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
