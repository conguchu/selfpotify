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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/**
 * Componentes compartidos del perfil (CLAUDE.md §2 — reutilizables entre features `profile`,
 * `detail` y `follow`): cabecera (avatar + nombre + contadores), tarjeta de cuadrícula de usuario,
 * diálogo de edición de nombre y hoja de opciones de la foto. Todos son *stateless* y reciben sus
 * callbacks por parámetro.
 */

/**
 * Avatar circular del perfil. Si [editable] superpone una insignia de cámara en la esquina y todo el
 * avatar es pulsable ([onClick]). Reutiliza [CircleAvatar] para el placeholder/carga.
 */
@Composable
fun ProfileAvatar(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    editable: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.size(size)) {
        val avatarModifier = if (onClick != null) {
            Modifier.fillMaxSize().clip(CircleShape).clickable(onClick = onClick)
        } else {
            Modifier.fillMaxSize()
        }
        CircleAvatar(url = url, modifier = avatarModifier)
        if (editable) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = stringResource(R.string.cd_change_photo),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Nombre visible (cae al username si está vacío) + `@username` debajo. Si [editable] muestra un
 * lápiz a la derecha que dispara [onEdit].
 */
@Composable
fun ProfileNameRow(
    displayName: String?,
    username: String?,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    onEdit: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = displayName?.takeIf { it.isNotBlank() } ?: username.orEmpty(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (editable && onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.cd_edit_name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (!displayName.isNullOrBlank() && !username.isNullOrBlank()) {
            Text(
                text = "@$username",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Contadores de seguidores y seguidos, cada uno una columna pulsable (estilo Spotify). */
@Composable
fun FollowCountsRow(
    followers: Int,
    following: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountColumn(followers, stringResource(R.string.profile_followers), onFollowersClick)
        CountColumn(following, stringResource(R.string.profile_following), onFollowingClick)
    }
}

@Composable
private fun CountColumn(count: Int, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.m, vertical = Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Celda de cuadrícula de un usuario: avatar circular + nombre + `@username`. Pulsar abre su perfil
 * ([onClick]). El slot [trailing] permite añadir un botón por fila (p. ej. *dejar de seguir*).
 */
@Composable
fun UserGridCard(
    user: UserSummaryDTO,
    serverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(Spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        CircleAvatar(
            url = ServerUrl.asset(serverUrl, user.avatarUrl),
            modifier = Modifier.size(88.dp),
        )
        Text(
            text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username.orEmpty(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!user.displayName.isNullOrBlank() && !user.username.isNullOrBlank()) {
            Text(
                text = "@${user.username}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) trailing()
    }
}

/** Diálogo para editar el nombre visible (máx. 120). Guarda con [onSave] (recibe el texto recortado). */
@Composable
fun EditNameDialog(
    initial: String,
    saving: Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 120) name = it },
                placeholder = { Text(stringResource(R.string.profile_edit_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = !saving) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.profile_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.profile_cancel))
            }
        },
    )
}

/**
 * Hoja inferior con las acciones de la foto de perfil: *Cambiar foto* (lanza el selector vía
 * [onChange]) y, si [hasPhoto], *Eliminar foto* ([onRemove]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarOptionsSheet(
    hasPhoto: Boolean,
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.l)) {
            AvatarOptionRow(
                icon = Icons.Rounded.PhotoCamera,
                label = stringResource(R.string.profile_change_photo),
                onClick = onChange,
            )
            if (hasPhoto) {
                AvatarOptionRow(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(R.string.profile_remove_photo),
                    onClick = onRemove,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AvatarOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.page, vertical = Spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(text = label, fontSize = 16.sp, color = tint)
    }
}
