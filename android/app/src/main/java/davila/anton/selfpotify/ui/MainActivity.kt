package davila.anton.selfpotify.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.ui.theme.BrandingColors
import davila.anton.selfpotify.ui.theme.SelfpotifyTheme
import davila.anton.selfpotify.ui.theme.ThemeViewModel
import kotlinx.coroutines.runBlocking

/**
 * Única Activity: aloja el NavHost de Compose. El destino inicial se decide según el estado
 * de la sesión persistida (CLAUDE.md §2):
 *  - sin servidor          -> pantalla de configuración de servidor
 *  - servidor pero sin JWT  -> login / registro
 *  - servidor + JWT válido  -> home
 *
 * El ThemeViewModel vive a nivel de Activity y la paleta del servidor se proyecta una sola
 * vez sobre SelfpotifyTheme, de modo que todas las pantallas comparten el branding.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = SessionStore(this)
        val session = runBlocking { store.current() }
        val startDestination = when {
            session.isLoggedIn -> Route.HOME
            session.hasServer -> Route.AUTH
            else -> Route.SERVER
        }

        val initial = BrandingColors.from(runBlocking { store.currentBrandingColors() })
        window.statusBarColor = initial.background
        window.navigationBarColor = initial.background

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val colors by themeViewModel.colors.collectAsStateWithLifecycle()
            SelfpotifyTheme(colors) {
                SelfpotifyApp(startDestination = startDestination)
            }
        }
    }
}
