package davila.anton.selfpotify

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade

/**
 * Application de Selfpotify. Su única responsabilidad es configurar el `ImageLoader` global de
 * Coil con cachés **acotadas**, para no agotar la memoria ni el almacenamiento del teléfono al
 * desplazar carruseles largos de carátulas en Descubrir:
 *
 * - **Memoria:** como mucho el 20 % del heap de la app (LRU; las carátulas que salen de pantalla
 *   se descartan), evitando los `OutOfMemoryError` al cargar muchas imágenes.
 * - **Disco:** tope de 50 MB en `cacheDir/image_cache`, para que la caché persistente no crezca
 *   sin límite entre sesiones.
 */
class SelfpotifyApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
