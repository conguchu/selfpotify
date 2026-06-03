package davila.anton.selfpotify.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector
import davila.anton.selfpotify.R

/** Las cuatro pestañas de la barra de navegación inferior (estilo Spotify, CLAUDE.md §3.2). */
enum class Tab(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    DISCOVER("tab_discover", R.string.tab_discover, Icons.Rounded.Explore),
    SEARCH("tab_search", R.string.tab_search, Icons.Rounded.Search),
    LIBRARY("tab_library", R.string.tab_library, Icons.Rounded.LibraryMusic),
    PROFILE("tab_profile", R.string.tab_profile, Icons.Rounded.Person),
}
