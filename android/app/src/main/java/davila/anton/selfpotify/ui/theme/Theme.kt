package davila.anton.selfpotify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tema Compose de Selfpotify (estilo Spotify oscuro). La paleta es **dinámica**: viene del
 * servidor como [BrandingColors] (CLAUDE.md §3.1) y aquí se proyecta sobre el `ColorScheme`
 * de Material 3, de modo que botones, campos y spinners adoptan el branding sin recolorear
 * vista a vista. Los tokens que Material no cubre (texto secundario, hover del acento, etc.)
 * se exponen aparte vía [LocalBrandingColors].
 */

/** Acceso a la paleta completa del servidor desde cualquier composable descendiente. */
val LocalBrandingColors = staticCompositionLocalOf { BrandingColors.fallback() }

/**
 * URL absoluta del logo del servidor (o `null` si no hay → logo local de fallback).
 * La consume el composable común `ServerLogo` para no propagar la URL pantalla a pantalla.
 */
val LocalServerLogoUrl = staticCompositionLocalOf<String?> { null }

/** Conversión directa de un entero ARGB de [BrandingColors] a [Color] de Compose. */
fun Int.toBrandingColor(): Color = Color(this)

@Composable
fun SelfpotifyTheme(
    colors: BrandingColors,
    logoUrl: String?,
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = colors.accent.toBrandingColor(),
        onPrimary = colors.onAccent.toBrandingColor(),
        secondary = colors.accent.toBrandingColor(),
        onSecondary = colors.onAccent.toBrandingColor(),
        background = colors.background.toBrandingColor(),
        onBackground = colors.textPrimary.toBrandingColor(),
        surface = colors.surface.toBrandingColor(),
        onSurface = colors.textPrimary.toBrandingColor(),
        surfaceVariant = colors.surfaceVariant.toBrandingColor(),
        onSurfaceVariant = colors.textSecondary.toBrandingColor(),
        outline = colors.border.toBrandingColor(),
        error = colors.error.toBrandingColor(),
        onError = colors.onAccent.toBrandingColor(),
    )
    CompositionLocalProvider(
        LocalBrandingColors provides colors,
        LocalServerLogoUrl provides logoUrl,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** Espaciado en múltiplos de 8 dp y métricas de la UI (CLAUDE.md §3.2). */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Padding lateral de página. */
    val page = 16.dp

    /** Tamaño del logo de cabecera. */
    val logo = 96.dp

    /** Altura de los botones principales (estilo Spotify). */
    val button = 52.dp
}
