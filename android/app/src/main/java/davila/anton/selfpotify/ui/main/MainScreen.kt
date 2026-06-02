package davila.anton.selfpotify.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import davila.anton.selfpotify.ui.detail.AlbumDetailScreen
import davila.anton.selfpotify.ui.detail.ArtistDetailScreen
import davila.anton.selfpotify.ui.detail.PlaylistDetailScreen
import davila.anton.selfpotify.ui.detail.UserDetailScreen
import davila.anton.selfpotify.ui.discover.DiscoverScreen
import davila.anton.selfpotify.ui.library.LibraryScreen
import davila.anton.selfpotify.ui.player.MiniPlayer
import davila.anton.selfpotify.ui.player.PlayerViewModel
import davila.anton.selfpotify.ui.profile.ProfileScreen
import davila.anton.selfpotify.ui.search.SearchScreen

/**
 * Contenedor principal de la app logueada (CLAUDE.md §3.2): `Scaffold` con un `NavHost` anidado
 * para las cuatro pestañas, una `NavigationBar` inferior y, encima de ella, el mini-player
 * persistente. Abrir el reproductor completo y navegar a login/servidor/sin-conexión se delega
 * al NavHost externo vía callbacks.
 */
@Composable
fun MainScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToServer: () -> Unit,
    onNavigateToOffline: () -> Unit,
    onOpenPlayer: () -> Unit,
    vm: MainViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val tabNavController = rememberNavController()

    LaunchedEffect(Unit) {
        vm.navigateOffline.collect { onNavigateToOffline() }
    }
    LaunchedEffect(Unit) {
        vm.sessionExpired.collect { onNavigateToAuth() }
    }
    LaunchedEffect(Unit) { vm.checkSession() }

    val backStack by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(vm = playerViewModel, onExpand = onOpenPlayer)
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    tabNavController.navigate(tab.route) {
                                        popUpTo(tabNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Navegación a detalle dentro del propio grafo de pestañas: la barra inferior y el
        // mini-player siguen visibles al abrir artista/álbum/playlist/usuario.
        val openArtist: (Long) -> Unit = { tabNavController.navigate(DetailRoute.artist(it)) }
        val openAlbum: (Long) -> Unit = { tabNavController.navigate(DetailRoute.album(it)) }
        val openPlaylist: (Long) -> Unit = { tabNavController.navigate(DetailRoute.playlist(it)) }
        val openUser: (Long) -> Unit = { tabNavController.navigate(DetailRoute.user(it)) }
        val onBack: () -> Unit = { tabNavController.popBackStack() }

        NavHost(
            navController = tabNavController,
            startDestination = Tab.DISCOVER.route,
        ) {
            composable(Tab.DISCOVER.route) {
                DiscoverScreen(contentPadding = innerPadding, onOpenArtist = openArtist)
            }
            composable(Tab.SEARCH.route) {
                SearchScreen(
                    contentPadding = innerPadding,
                    onOpenArtist = openArtist,
                    onOpenAlbum = openAlbum,
                    onOpenPlaylist = openPlaylist,
                    onOpenUser = openUser,
                )
            }
            composable(Tab.LIBRARY.route) { LibraryScreen(contentPadding = innerPadding) }
            composable(Tab.PROFILE.route) {
                ProfileScreen(
                    contentPadding = innerPadding,
                    onNavigateToAuth = onNavigateToAuth,
                    onNavigateToServer = onNavigateToServer,
                )
            }

            composable(
                DetailRoute.ARTIST,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                ArtistDetailScreen(
                    id = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    contentPadding = innerPadding,
                    onBack = onBack,
                )
            }
            composable(
                DetailRoute.ALBUM,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                AlbumDetailScreen(
                    id = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    contentPadding = innerPadding,
                    onBack = onBack,
                )
            }
            composable(
                DetailRoute.PLAYLIST,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                PlaylistDetailScreen(
                    id = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    contentPadding = innerPadding,
                    onBack = onBack,
                )
            }
            composable(
                DetailRoute.USER,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                UserDetailScreen(
                    id = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    contentPadding = innerPadding,
                    onBack = onBack,
                    onOpenPlaylist = openPlaylist,
                )
            }
        }
    }
}
