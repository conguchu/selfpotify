package davila.anton.selfpotify.ui.theme

import android.graphics.Color

/**
 * Paleta de marca resuelta a enteros ARGB, lista para aplicar en runtime (CLAUDE.md §3.1).
 *
 * Se deriva del mapa de tokens CSS que envía el servidor en `branding.colors`
 * (`GET /api/config/public`). Es una preocupación de UI —usa [android.graphics.Color]—
 * por eso vive en `ui/theme` y no en `data/model` (que solo guarda el mapa crudo).
 *
 * Cualquier token ausente cae al valor de fallback de carga, que debe coincidir con
 * `res/values/colors.xml`.
 */
data class BrandingColors(
    val background: Int,
    val surface: Int,
    val surfaceVariant: Int,
    val border: Int,
    val accent: Int,
    val accentHover: Int,
    val onAccent: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val error: Int,
) {
    companion object {

        /** Fallback de carga; debe reflejar `res/values/colors.xml` (CLAUDE.md §3.1). */
        fun fallback() = BrandingColors(
            background = 0xFF121212.toInt(),
            surface = 0xFF181818.toInt(),
            surfaceVariant = 0xFF282828.toInt(),
            border = 0xFF282828.toInt(),
            accent = 0xFF1DB954.toInt(),
            accentHover = 0xFF1DB954.toInt(),
            onAccent = 0xFF000000.toInt(),
            textPrimary = 0xFFFFFFFF.toInt(),
            textSecondary = 0xFFB3B3B3.toInt(),
            error = 0xFFF15E6C.toInt(),
        )

        /**
         * Construye la paleta a partir del mapa de tokens del servidor. Las claves siguen
         * los nombres reales de `ServerGlobalConfig.defaultColors()` (`--color-bg`, …).
         */
        fun from(tokens: Map<String, String>?): BrandingColors {
            if (tokens.isNullOrEmpty()) return fallback()
            val fb = fallback()
            fun token(key: String, default: Int) = parse(tokens[key]) ?: default
            val accent = token("--color-accent", fb.accent)
            return BrandingColors(
                background = token("--color-bg", fb.background),
                surface = token("--color-bg-card", fb.surface),
                surfaceVariant = token("--color-bg-hover", fb.surfaceVariant),
                border = token("--color-border", fb.border),
                accent = accent,
                accentHover = token("--color-accent-hover", accent),
                // El servidor no envía on-accent: se elige negro/blanco por contraste.
                onAccent = onAccentFor(accent),
                textPrimary = token("--color-text", fb.textPrimary),
                textSecondary = token("--color-text-muted", fb.textSecondary),
                error = token("--color-danger", fb.error),
            )
        }

        /** Negro sobre acentos claros, blanco sobre oscuros (luminancia relativa WCAG). */
        private fun onAccentFor(accent: Int): Int {
            fun channel(value: Int): Double {
                val c = value / 255.0
                return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            val luminance = 0.2126 * channel(Color.red(accent)) +
                0.7152 * channel(Color.green(accent)) +
                0.0722 * channel(Color.blue(accent))
            return if (luminance > 0.4) Color.BLACK else Color.WHITE
        }

        /** Acepta `#RGB` y `#RRGGBB` (con o sin almohadilla). Devuelve null si no es válido. */
        private fun parse(hex: String?): Int? {
            val raw = hex?.trim()?.removePrefix("#")?.takeIf { it.isNotEmpty() } ?: return null
            val normalized = when (raw.length) {
                3 -> raw.map { "$it$it" }.joinToString(separator = "")
                6 -> raw
                else -> return null
            }
            return runCatching { Color.parseColor("#$normalized") }.getOrNull()
        }
    }
}
