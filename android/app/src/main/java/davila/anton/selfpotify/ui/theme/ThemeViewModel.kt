package davila.anton.selfpotify.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import davila.anton.selfpotify.data.local.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Expone la paleta de marca del servidor como `StateFlow<BrandingColors>` (CLAUDE.md §3.1).
 *
 * Se comparte a nivel de Activity (`by activityViewModels()`) para que las tres pantallas
 * del flujo de acceso adopten los colores del servidor en cuanto se persisten —en la
 * validación del servidor y al iniciar sesión—. Mientras no haya paleta guardada emite el
 * fallback de carga.
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
}
