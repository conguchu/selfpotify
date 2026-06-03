package davila.anton.selfpotify.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.AvatarOptionsSheet
import davila.anton.selfpotify.ui.common.EditNameDialog
import davila.anton.selfpotify.ui.common.FollowCountsRow
import davila.anton.selfpotify.ui.common.ProfileAvatar
import davila.anton.selfpotify.ui.common.ProfileNameRow
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/**
 * Pestaña Perfil (propio). Muestra avatar y nombre **editables** (tocar la foto abre la hoja de
 * opciones; el lápiz abre el diálogo de nombre), los contadores de seguidores/seguidos —que abren
 * sus cuadrículas— y, abajo, **cerrar sesión** y **cambiar de servidor**. No muestra playlists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onNavigateToAuth: () -> Unit,
    onNavigateToServer: () -> Unit,
    onOpenFollowers: (Long) -> Unit,
    onOpenFollowing: (Long) -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.navigate.collect { dest ->
            when (dest) {
                ProfileNav.TO_AUTH -> onNavigateToAuth()
                ProfileNav.TO_SERVER -> onNavigateToServer()
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) vm.changePhoto(uri) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.page),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            Spacer(Modifier.height(Spacing.xxl))
            Box(contentAlignment = Alignment.Center) {
                ProfileAvatar(
                    url = ServerUrl.asset(state.serverUrl, state.avatarUrl),
                    editable = true,
                    onClick = vm::openPhotoSheet,
                )
                if (state.uploadingPhoto) {
                    Box(
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            Spacer(Modifier.height(Spacing.m))
            ProfileNameRow(
                displayName = state.displayName,
                username = state.username,
                editable = true,
                onEdit = vm::openNameDialog,
            )
            Spacer(Modifier.height(Spacing.m))
            FollowCountsRow(
                followers = state.followersCount,
                following = state.followingCount,
                onFollowersClick = { state.id?.let(onOpenFollowers) },
                onFollowingClick = { state.id?.let(onOpenFollowing) },
            )
            Spacer(Modifier.height(Spacing.xxl))
            Button(
                onClick = { vm.logout() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(Spacing.button),
            ) {
                Text(stringResource(R.string.profile_logout))
            }
            Spacer(Modifier.height(Spacing.m))
            OutlinedButton(
                onClick = { vm.changeServer() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(Spacing.button),
            ) {
                Text(stringResource(R.string.profile_change_server))
            }
            }
        }
    }

    if (state.showNameDialog) {
        EditNameDialog(
            initial = state.displayName.orEmpty(),
            saving = state.savingName,
            onSave = vm::saveName,
            onDismiss = vm::closeNameDialog,
        )
    }

    if (state.showPhotoSheet) {
        AvatarOptionsSheet(
            hasPhoto = !state.avatarUrl.isNullOrBlank(),
            onChange = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onRemove = vm::removePhoto,
            onDismiss = vm::closePhotoSheet,
        )
    }
}
