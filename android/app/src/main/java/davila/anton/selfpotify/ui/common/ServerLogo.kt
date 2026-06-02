package davila.anton.selfpotify.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.theme.LocalServerLogoUrl

/**
 * Logo de cabecera de la app. Carga el **logo del servidor** (`branding.logoUrl`, resuelto a
 * URL absoluta y expuesto vía [LocalServerLogoUrl]) en lugar del logo de Selfpotify.
 *
 * El logo local empaquetado (`R.drawable.logo_selfpotify`) es solo un **fallback de carga**:
 * se muestra mientras llega la imagen del servidor, si la descarga falla, o si el servidor no
 * define logo. Así toda la app adopta la marca del servidor al que se conecta (igual que la
 * paleta de colores; ver README, sección Android → "Branding dinámico del servidor").
 */
@Composable
fun ServerLogo(modifier: Modifier = Modifier) {
    val url = LocalServerLogoUrl.current
    val fallback = painterResource(R.drawable.logo_selfpotify)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.cd_logo),
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
