package davila.anton.selfpotify.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.detail.AlbumDetailScreen
import davila.anton.selfpotify.ui.detail.ArtistDetailScreen
import davila.anton.selfpotify.ui.detail.PlaylistDetailScreen
import davila.anton.selfpotify.ui.detail.UserDetailScreen
import davila.anton.selfpotify.ui.discover.DiscoverScreen
import davila.anton.selfpotify.ui.follow.FollowListScreen
import davila.anton.selfpotify.ui.follow.FollowListType
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
    pendingArtistId: Long? = null,
    onPendingArtistConsumed: () -> Unit = {},
    pendingShareToken: String? = null,
    onShareTokenConsumed: () -> Unit = {},
    vm: MainViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
) {
    val tabNavController = rememberNavController()
    val context = LocalContext.current

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
        val openFollowers: (Long) -> Unit = { tabNavController.navigate(DetailRoute.followers(it)) }
        val openFollowing: (Long) -> Unit = { tabNavController.navigate(DetailRoute.following(it)) }
        val onBack: () -> Unit = { tabNavController.popBackStack() }

        // Abre el artista solicitado desde el reproductor una vez este ha colapsado a este
        // contenedor (la navegación a detalle vive en el grafo de pestañas, no en el externo).
        LaunchedEffect(pendingArtistId) {
            pendingArtistId?.let {
                openArtist(it)
                onPendingArtistConsumed()
            }
        }

        // Canje del deep link de invitación (selfpotify://playlist/share/{token}). Como MainScreen
        // solo existe con sesión iniciada, un token que llegue sin sesión espera en el estado de la
        // Activity hasta que el usuario entra y este contenedor se monta.
        LaunchedEffect(pendingShareToken) {
            pendingShareToken?.let { vm.redeemShare(it) }
        }
        LaunchedEffect(Unit) {
            vm.openPlaylist.collect { id ->
                openPlaylist(id)
                onShareTokenConsumed()
            }
        }
        LaunchedEffect(Unit) {
            vm.shareError.collect {
                Toast.makeText(
                    context,
                    context.getString(R.string.share_redeem_error),
                    Toast.LENGTH_LONG,
                ).show()
                onShareTokenConsumed()
            }
        }

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
            composable(Tab.LIBRARY.route) {
                LibraryScreen(contentPadding = innerPadding, onOpenPlaylist = openPlaylist)
            }
            composable(Tab.PROFILE.route) {
                ProfileScreen(
                    contentPadding = innerPadding,
                    onNavigateToAuth = onNavigateToAuth,
                    onNavigateToServer = onNavigateToServer,
                    onOpenFollowers = openFollowers,
                    onOpenFollowing = openFollowing,
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
                    onOpenFollowers = openFollowers,
                    onOpenFollowing = openFollowing,
                )
            }
            composable(
                DetailRoute.FOLLOWERS,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                FollowListScreen(
                    userId = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    type = FollowListType.FOLLOWERS,
                    contentPadding = innerPadding,
                    onBack = onBack,
                    onOpenUser = openUser,
                )
            }
            composable(
                DetailRoute.FOLLOWING,
                arguments = listOf(navArgument(DetailRoute.ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                FollowListScreen(
                    userId = entry.arguments?.getLong(DetailRoute.ARG_ID) ?: 0L,
                    type = FollowListType.FOLLOWING,
                    contentPadding = innerPadding,
                    onBack = onBack,
                    onOpenUser = openUser,
                )
            }
        }
    }
}
