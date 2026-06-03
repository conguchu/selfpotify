package davila.anton.selfpotify.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Expone la marca del servidor (paleta + logo) a nivel de Activity (CLAUDE.md §3.1).
 *
 * Se comparte a nivel de Activity (viewModel() en MainActivity) para que todas las pantallas
 * del flujo adopten los colores y el logo del servidor en cuanto se persisten. Mientras no
 * haya marca guardada emite el fallback de carga (paleta) y `null` (logo → logo local).
 */
class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val session = SessionStore(app)

    val colors: StateFlow<BrandingColors> = session.brandingColors
        .map { BrandingColors.from(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BrandingColors.fallback(),
        )

    /**
     * URL absoluta del logo del servidor, lista para cargar con Coil. Combina la dirección del
     * servidor activo con `branding.logoUrl`. Es `null` mientras no haya logo (la UI cae al
     * logo local empaquetado).
     */
    val logoUrl: StateFlow<String?> =
        combine(session.session, session.brandingLogoUrl) { s, logo ->
            ServerUrl.asset(s.serverUrl, logo)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
