package davila.anton.selfpotify.ui.common

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.model.UserSummaryDTO
import davila.anton.selfpotify.ui.theme.Spacing
import davila.anton.selfpotify.util.ServerUrl

/**
 * Hoja inferior para compartir una playlist por **magic link** de un solo uso (README §454-464).
 * Presentacional: el ViewModel genera el enlace y la lista de colaboradores y los pasa aquí. Permite
 * copiar el enlace, generar otro y quitar colaboradores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePlaylistSheet(
    loading: Boolean,
    error: Boolean,
    shareUrl: String?,
    collaborators: List<UserSummaryDTO>,
    serverUrl: String?,
    onRegenerate: () -> Unit,
    onRemoveCollaborator: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val copiedMessage = stringResource(R.string.share_copied)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(
                text = stringResource(R.string.playlist_share_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.share_description),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                loading -> Box(Modifier.fillMaxWidth().padding(Spacing.l), Alignment.Center) {
                    CircularProgressIndicator()
                }

                error || shareUrl == null -> Text(
                    text = stringResource(R.string.share_error),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )

                else -> {
                    OutlinedTextField(
                        value = shareUrl,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.share_link_label)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(shareUrl))
                                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.share_copy),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = onRegenerate, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.share_regenerate),
                            modifier = Modifier.padding(start = Spacing.s),
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.share_collaborators),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (collaborators.isEmpty()) {
                Text(
                    text = stringResource(R.string.share_no_collaborators),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                ) {
                    items(collaborators, key = { it.id }) { user ->
                        CollaboratorRow(user, serverUrl) { onRemoveCollaborator(user.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollaboratorRow(user: UserSummaryDTO, serverUrl: String?, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        CircleAvatar(
            url = ServerUrl.asset(serverUrl, user.avatarUrl),
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username.orEmpty(),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.share_remove_collaborator),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
