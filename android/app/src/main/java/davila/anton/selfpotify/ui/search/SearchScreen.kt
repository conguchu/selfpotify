package davila.anton.selfpotify.ui.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.PlaceholderScreen

/** Pestaña Búsqueda — placeholder en esta tanda (la búsqueda real llegará después). */
@Composable
fun SearchScreen(contentPadding: PaddingValues) {
    PlaceholderScreen(
        icon = Icons.Rounded.Search,
        title = stringResource(R.string.search_title),
        subtitle = stringResource(R.string.coming_soon),
        contentPadding = contentPadding,
    )
}
