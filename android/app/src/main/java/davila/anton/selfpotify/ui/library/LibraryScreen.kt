package davila.anton.selfpotify.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import davila.anton.selfpotify.ui.common.CoverImage
import davila.anton.selfpotify.ui.common.PlaylistFormSheet
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/**
 * Pestaña Biblioteca (estilo Spotify): una tarjeta de "nueva playlist" arriba y, debajo, las
 * playlists propias y las compartidas conmigo en tarjetas con carátula, nombre y descripción.
 */
@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    onOpenPlaylist: (Long) -> Unit,
    vm: LibraryViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(Unit) { vm.created.collect { onOpenPlaylist(it) } }
    val state by vm.state.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when {
            state.loading -> CenterLoader()
            state.error -> CenterMessage(stringResource(R.string.library_error))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding() + Spacing.s,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.l,
                ),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.library_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.s),
                    )
                }
                item { AddPlaylistCard(onClick = vm::openCreate) }

                if (state.myPlaylists.isEmpty() && state.sharedPlaylists.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.library_empty),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.page),
                        )
                    }
                }

                if (state.myPlaylists.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.library_my_playlists)) }
                    items(state.myPlaylists, key = { "my-${it.id}" }) { playlist ->
                        PlaylistCard(playlist, state.serverUrl) { onOpenPlaylist(playlist.id) }
                    }
                }
                if (state.sharedPlaylists.isNotEmpty()) {
                    item { SectionHeader(stringResource(R.string.library_shared)) }
                    items(state.sharedPlaylists, key = { "shared-${it.id}" }) { playlist ->
                        PlaylistCard(playlist, state.serverUrl) { onOpenPlaylist(playlist.id) }
                    }
                }
            }
        }
    }

    if (state.creating) {
        PlaylistFormSheet(
            editing = false,
            saving = state.saving,
            error = state.formError,
            onSave = { name, description, isPublic, coverUri, _ ->
                vm.createPlaylist(name, description, isPublic, coverUri)
            },
            onDismiss = vm::closeCreate,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.page, vertical = Spacing.s),
    )
}

@Composable
private fun AddPlaylistCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.page, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = stringResource(R.string.library_add_playlist),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun PlaylistCard(playlist: PlaylistDTO, serverUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.page, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        CoverImage(
            url = ServerUrl.asset(serverUrl, playlist.pictureUrl),
            modifier = Modifier.size(56.dp),
            cornerRadius = 8.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = playlist.name.orEmpty(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = playlist.description?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.playlist_song_count, playlist.songIds?.size ?: 0)
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!playlist.isPublic) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        if (playlist.isShared) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = stringResource(R.string.cd_playlist_shared),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
