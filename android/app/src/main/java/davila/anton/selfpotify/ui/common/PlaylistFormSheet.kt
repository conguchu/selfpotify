package davila.anton.selfpotify.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.theme.Spacing

/**
 * Hoja inferior reutilizable para **crear** o **editar** una playlist (CLAUDE.md §3.2). Es
 * presentacional: el estado de carga/error y la persistencia los gobierna el ViewModel del caller.
 * Devuelve los datos del formulario por [onSave]; la `Uri` de carátula (si el usuario eligió una
 * nueva con el Photo Picker) va aparte para que el caller la lea y suba con `POST .../cover`.
 *
 * @param editing `true` en modo edición: cambia título/botón y muestra el borrado.
 * @param currentCoverUrl carátula actual (solo edición) para previsualizar si no se elige otra.
 * @param onDelete acción de borrado (solo edición); `null` la oculta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistFormSheet(
    editing: Boolean,
    saving: Boolean,
    error: Boolean,
    initialName: String = "",
    initialDescription: String = "",
    initialPublic: Boolean = false,
    currentCoverUrl: String? = null,
    onSave: (name: String, description: String?, isPublic: Boolean, coverUri: Uri?) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }
    var isPublic by rememberSaveable { mutableStateOf(initialPublic) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) coverUri = uri }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = Spacing.page)
                .padding(bottom = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        if (editing) R.string.playlist_form_edit_title else R.string.playlist_form_create_title,
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null) {
                    IconButton(onClick = { confirmDelete = true }, enabled = !saving) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.playlist_form_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            CoverPicker(
                coverUri = coverUri,
                currentCoverUrl = currentCoverUrl,
                onPick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_form_name)) },
                placeholder = { Text(stringResource(R.string.playlist_form_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.playlist_form_description)) },
                placeholder = { Text(stringResource(R.string.playlist_form_description_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.playlist_form_public),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.playlist_form_public_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isPublic, onCheckedChange = { isPublic = it }, enabled = !saving)
            }

            if (error) {
                Text(
                    text = stringResource(R.string.playlist_form_error),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.playlist_form_cancel))
                }
                Button(
                    onClick = {
                        onSave(name.trim(), description.trim().ifBlank { null }, isPublic, coverUri)
                    },
                    enabled = name.isNotBlank() && !saving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (editing) R.string.playlist_form_save else R.string.playlist_form_create,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.playlist_delete_confirm_title)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(
                        stringResource(R.string.playlist_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.playlist_form_cancel))
                }
            },
        )
    }
}

/** Cuadrado pulsable que previsualiza la carátula elegida ([coverUri]) o la actual ([currentCoverUrl]). */
@Composable
private fun CoverPicker(coverUri: Uri?, currentCoverUrl: String?, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        val model: Any? = coverUri ?: currentCoverUrl
        if (model != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).build(),
                contentDescription = stringResource(R.string.playlist_form_cover),
                contentScale = ContentScale.Crop,
                loading = { PickIcon() },
                error = { PickIcon() },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PickIcon()
        }
    }
}

@Composable
private fun PickIcon() {
    Icon(
        imageVector = Icons.Rounded.AddPhotoAlternate,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(36.dp),
    )
}
