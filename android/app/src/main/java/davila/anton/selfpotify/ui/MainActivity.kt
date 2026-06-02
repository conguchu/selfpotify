package davila.anton.selfpotify.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.network.ApiProvider
import davila.anton.selfpotify.data.repository.StreamTokenRepository
import davila.anton.selfpotify.playback.PlaybackConnection
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

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = SessionStore(this)

        // El JWT vigente alimenta el AuthInterceptor de Retrofit; el reproductor se conecta al
        // servicio Media3 y construye sus URLs con un stream token.
        ApiProvider.init { runBlocking { store.current().token } }
        PlaybackConnection.init(this, StreamTokenRepository(store), store)

        val session = runBlocking { store.current() }
        val startDestination = when {
            session.isLoggedIn -> Route.HOME
            session.hasServer -> Route.AUTH
            else -> Route.SERVER
        }

        // Notificación del reproductor en Android 13+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val initial = BrandingColors.from(runBlocking { store.currentBrandingColors() })
        window.statusBarColor = initial.background
        window.navigationBarColor = initial.background

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val colors by themeViewModel.colors.collectAsStateWithLifecycle()
            val logoUrl by themeViewModel.logoUrl.collectAsStateWithLifecycle()
            SelfpotifyTheme(colors, logoUrl) {
                SelfpotifyApp(startDestination = startDestination)
            }
        }
    }
}
