package davila.anton.selfpotify.ui.theme

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout

/**
 * Helpers para aplicar [BrandingColors] sobre vistas concretas en runtime.
 *
 * En el View system la paleta dinámica no puede inyectarse como atributos de tema con
 * valores hex arbitrarios, así que se recolorean las vistas programáticamente
 * (CLAUDE.md §3.1: "ColorStateList programáticos").
 */

/** Botón de acción principal: relleno con el acento y texto on-accent (atenuado si está deshabilitado). */
fun MaterialButton.applyFilled(c: BrandingColors) {
    backgroundTintList = stateList(c.accent)
    setTextColor(stateList(c.onAccent))
    iconTint = ColorStateList.valueOf(c.onAccent)
    rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(c.onAccent, RIPPLE_ALPHA))
}

/** Botón con borde: contorno + texto en acento. */
fun MaterialButton.applyOutlined(c: BrandingColors) {
    setTextColor(stateList(c.accent))
    strokeColor = ColorStateList.valueOf(c.border)
    iconTint = ColorStateList.valueOf(c.accent)
    rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(c.accent, RIPPLE_ALPHA))
}

/** Botón de texto plano; [color] permite usar acento (acción) o texto secundario (acción menor). */
fun MaterialButton.applyText(c: BrandingColors, color: Int = c.accent) {
    setTextColor(color)
    rippleColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, RIPPLE_ALPHA))
}

/** Campo de texto Material: contorno y cursor en acento, hint en texto secundario, texto primario. */
fun TextInputLayout.applyBranding(c: BrandingColors) {
    val accent = ColorStateList.valueOf(c.accent)
    val secondary = ColorStateList.valueOf(c.textSecondary)
    boxStrokeColor = c.accent
    hintTextColor = accent
    defaultHintTextColor = secondary
    setHelperTextColor(secondary)
    setEndIconTintList(secondary)
    editText?.apply {
        setTextColor(c.textPrimary)
        setHintTextColor(c.textSecondary)
        runCatching { textCursorDrawable?.setTint(c.accent) }
    }
}

/** Tiñe un spinner indeterminado con el acento. */
fun ProgressBar.applyBranding(c: BrandingColors) {
    indeterminateTintList = ColorStateList.valueOf(c.accent)
}

private const val RIPPLE_ALPHA = 0x33

/** ColorStateList que atenúa el color al ~38 % cuando la vista está deshabilitada. */
private fun stateList(color: Int): ColorStateList {
    val disabled = ColorUtils.setAlphaComponent(color, DISABLED_ALPHA)
    return ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf(disabled, color),
    )
}

private const val DISABLED_ALPHA = 0x61
