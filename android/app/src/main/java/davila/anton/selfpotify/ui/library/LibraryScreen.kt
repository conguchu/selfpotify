package davila.anton.selfpotify.ui.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.PlaceholderScreen

/** Pestaña Biblioteca — placeholder en esta tanda (gestión de playlists llegará después). */
@Composable
fun LibraryScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        icon = Icons.Rounded.LibraryMusic,
        title = stringResource(R.string.library_title),
        subtitle = stringResource(R.string.coming_soon),
        contentPadding = contentPadding,
    )
}
