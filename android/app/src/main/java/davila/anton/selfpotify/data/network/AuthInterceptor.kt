package davila.anton.selfpotify.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adjunta `Authorization: Bearer <jwt>` a las peticiones cuando hay sesión (CLAUDE.md §5).
 *
 * El token se obtiene de [tokenProvider] en cada petición (no se captura una vez), de modo que
 * tras un login o logout las llamadas usan el token vigente sin recrear el cliente.
 *
 * El streaming (`/api/listen/{id}`) NO pasa por Retrofit: lo reproduce ExoPlayer con la URL
 * `?st=<streamToken>`, así que este interceptor no le afecta.
 */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
