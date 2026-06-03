package davila.anton.selfpotify.ui

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import davila.anton.selfpotify.ui.auth.AuthScreen
import davila.anton.selfpotify.ui.main.MainScreen
import davila.anton.selfpotify.ui.offline.ConnectionLostScreen
import davila.anton.selfpotify.ui.player.PlayerScreen
import davila.anton.selfpotify.ui.server.ServerSetupScreen

/**
 * NavHost de la aplicación. Una sola Activity + grafo de Compose Navigation.
 * El grafo refleja el flujo documentado en el README (sección Android).
 */
@Composable
fun SelfpotifyApp(
    startDestination: String,
    pendingShareToken: String? = null,
    onShareTokenConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()

    // Artista pendiente de abrir solicitado desde el reproductor (NavHost externo). El detalle de
    // artista vive en el grafo de pestañas de MainScreen, así que el reproductor colapsa y delega
    // la navegación en MainScreen vía este estado compartido.
    var pendingArtistId by rememberSaveable { mutableStateOf<Long?>(null) }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Route.SERVER) {
            ServerSetupScreen(
                onNavigateToAuth = {
                    navController.navigate(Route.AUTH) {
                        popUpTo(Route.SERVER) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.AUTH) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.AUTH) { inclusive = true }
                    }
                },
                onNavigateToServer = {
                    navController.navigate(Route.SERVER) {
                        popUpTo(Route.AUTH) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.HOME) {
            MainScreen(
                onNavigateToAuth = {
                    navController.navigate(Route.AUTH) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                },
                onNavigateToServer = {
                    navController.navigate(Route.SERVER) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                },
                onNavigateToOffline = {
                    navController.navigate(Route.OFFLINE) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                },
                onOpenPlayer = { navController.navigate(Route.PLAYER) },
                pendingArtistId = pendingArtistId,
                onPendingArtistConsumed = { pendingArtistId = null },
                pendingShareToken = pendingShareToken,
                onShareTokenConsumed = onShareTokenConsumed,
            )
        }

        composable(
            Route.PLAYER,
            enterTransition = { slideInVertically { it } },
            exitTransition = { slideOutVertically { it } },
            popEnterTransition = { slideInVertically { it } },
            popExitTransition = { slideOutVertically { it } },
        ) {
            PlayerScreen(
                onCollapse = { navController.popBackStack() },
                onOpenArtist = { id ->
                    pendingArtistId = id
                    navController.popBackStack()
                },
            )
        }

        composable(Route.OFFLINE) {
            ConnectionLostScreen(
                onNavigateToHome = {
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.OFFLINE) { inclusive = true }
                    }
                },
                onNavigateToServer = {
                    navController.navigate(Route.SERVER) {
                        popUpTo(Route.OFFLINE) { inclusive = true }
                    }
                },
            )
        }
    }
}
