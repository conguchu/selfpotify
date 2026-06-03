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
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

/** Pantalla de playlist: carátula/nombre/descripción + canciones numeradas, edición y compartir. */
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

    // Snackbar «se ha eliminado X, pulsa para deshacer». Las canciones usan el host de la pantalla;
    // los colaboradores, el host del propio bottom sheet de compartir (si no, quedaría oculto tras él).
    val snackbarHostState = remember { SnackbarHostState() }
    val shareSnackbarHostState = remember { SnackbarHostState() }
    val removedTemplate = stringResource(R.string.playlist_track_removed)
    val undoLabel = stringResource(R.string.action_undo)
    val unknownTitle = stringResource(R.string.playlist_track_unknown)
    LaunchedEffect(Unit) {
        vm.undo.collect { ev ->
            val title = ev.title?.takeIf { it.isNotBlank() } ?: unknownTitle
            val host = when (ev) {
                is PlaylistDetailViewModel.UndoEvent.Song -> snackbarHostState
                is PlaylistDetailViewModel.UndoEvent.Collaborator -> shareSnackbarHostState
            }
            val result = host.showSnackbar(
                message = String.format(removedTemplate, title),
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                when (ev) {
                    is PlaylistDetailViewModel.UndoEvent.Song -> vm.undoRemoveSong(ev.songId)
                    is PlaylistDetailViewModel.UndoEvent.Collaborator -> vm.undoRemoveCollaborator(ev.userId)
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(top = contentPadding.calculateTopPadding())) {
            DetailTopBar(title = state.playlist?.name.orEmpty(), onBack = onBack)
            when {
                state.loading -> CenterLoader()
                state.error || state.playlist == null -> CenterMessage(stringResource(R.string.detail_error))
                else -> {
                    val playlist = state.playlist!!
                    val subtitle = playlist.description?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.playlist_song_count, state.songs.size)
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
                                    position = index + 1,
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding()),
        )
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
            onSave = { name, description, isPublic, coverUri, removeCover ->
                vm.savePlaylist(name, description, isPublic, coverUri, removeCover)
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
            snackbarHostState = shareSnackbarHostState,
            onRegenerate = vm::generateShareLink,
            onRemoveCollaborator = vm::removeCollaborator,
            onDismiss = vm::closeShare,
        )
    }
}

/**
 * Fila de acciones bajo la cabecera.
 * - Colaborador (no dueño): icono estático «compartida» si la playlist tiene colaboradores.
 * - Dueño: botón editar + botón de gestión de colaboradores (icono Group); el icono estático
 *   no se muestra para evitar duplicados (el botón ya implica que está compartida).
 */
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
        if (shared && !isOwner) {
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
                    imageVector = Icons.Rounded.Group,
                    contentDescription = stringResource(R.string.cd_playlist_share),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
