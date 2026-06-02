package davila.anton.selfpotify.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.CenterLoader
import davila.anton.selfpotify.ui.common.CenterMessage
import davila.anton.selfpotify.ui.common.DetailHeader
import davila.anton.selfpotify.ui.common.DetailTopBar
import davila.anton.selfpotify.ui.common.PlaylistFormSheet
import davila.anton.selfpotify.ui.common.SharePlaylistSheet
import davila.anton.selfpotify.ui.common.SongRow
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/** Pantalla de playlist: carátula/nombre/descripción + canciones, con edición y compartir si soy dueño. */
@Composable
fun PlaylistDetailScreen(
    id: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    vm: PlaylistDetailViewModel = viewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    LaunchedEffect(Unit) { vm.deleted.collect { onBack() } }
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
                    // Dueño o colaborador pueden editar el contenido (añadir/quitar canciones).
                    val canEditContent = state.isOwner ||
                        (playlist.collaboratorIds?.contains(state.currentUserId) == true)
                    LazyColumn(contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())) {
                        item {
                            DetailHeader(
                                coverUrl = ServerUrl.asset(state.serverUrl, playlist.pictureUrl),
                                title = playlist.name.orEmpty(),
                                subtitle = subtitle,
                                circular = false,
                            )
                        }
                        item {
                            PlaylistActions(
                                shared = playlist.isShared,
                                isOwner = state.isOwner,
                                onEdit = vm::startEdit,
                                onShare = vm::startShare,
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
                                    listeners = song.listeners,
                                    onRemoveFromPlaylist = if (canEditContent) {
                                        { vm.removeSong(song.id) }
                                    } else {
                                        null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val playlist = state.playlist
    if (state.editing && playlist != null) {
        PlaylistFormSheet(
            editing = true,
            saving = state.saving,
            error = state.formError,
            initialName = playlist.name.orEmpty(),
            initialDescription = playlist.description.orEmpty(),
            initialPublic = playlist.isPublic,
            currentCoverUrl = ServerUrl.asset(state.serverUrl, playlist.pictureUrl),
            onSave = { name, description, isPublic, coverUri ->
                vm.savePlaylist(name, description, isPublic, coverUri)
            },
            onDelete = vm::deletePlaylist,
            onDismiss = vm::closeEdit,
        )
    }
    if (state.sharing) {
        SharePlaylistSheet(
            loading = state.shareLoading,
            error = state.shareError,
            shareUrl = state.shareUrl,
            collaborators = state.collaborators,
            serverUrl = state.serverUrl,
            onRegenerate = vm::generateShareLink,
            onRemoveCollaborator = vm::removeCollaborator,
            onDismiss = vm::closeShare,
        )
    }
}

/** Fila de acciones bajo la cabecera: icono "compartida" (informativo) y, si soy dueño, editar y compartir. */
@Composable
private fun PlaylistActions(
    shared: Boolean,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onShare: () -> Unit,
) {
    if (!shared && !isOwner) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.page),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shared) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = stringResource(R.string.cd_playlist_shared),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.s),
            )
        }
        if (isOwner) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.cd_playlist_edit),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = stringResource(R.string.cd_playlist_share),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
