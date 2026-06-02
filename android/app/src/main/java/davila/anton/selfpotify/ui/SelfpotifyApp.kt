package davila.anton.selfpotify.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import davila.anton.selfpotify.ui.auth.AuthScreen
import davila.anton.selfpotify.ui.home.HomeScreen
import davila.anton.selfpotify.ui.offline.ConnectionLostScreen
import davila.anton.selfpotify.ui.server.ServerSetupScreen

/**
 * NavHost de la aplicación. Una sola Activity + grafo de Compose Navigation.
 * El grafo refleja el flujo documentado en el README (sección Android).
 */
@Composable
fun SelfpotifyApp(startDestination: String) {
    val navController = rememberNavController()

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
            HomeScreen(
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
