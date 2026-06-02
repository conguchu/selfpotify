package davila.anton.selfpotify.data.network

import davila.anton.selfpotify.util.ServerUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye (y cachea) un [SelfpotifyApi] para una base URL dada.
 *
 * La URL del servidor se decide en tiempo de ejecución (CLAUDE.md §5), por lo que el
 * cliente Retrofit se reconstruye cuando cambia el servidor. Se cachea el último para
 * no recrearlo en cada llamada al mismo servidor.
 *
 * El JWT se adjunta vía [AuthInterceptor], que lee el token actual con [tokenProvider]
 * (registrado en [init] al arrancar la app). Así las peticiones autenticadas (feed, playlists,
 * perfil, stream token…) llevan `Authorization` sin acoplar la red a la sesión.
 */
object ApiProvider {

    @Volatile
    private var cache: Pair<String, SelfpotifyApi>? = null

    @Volatile
    private var tokenProvider: () -> String? = { null }

    /**
     * Registra el proveedor del JWT vigente. Se llama una vez al arrancar (MainActivity),
     * pasándole un lector de la sesión persistida.
     */
    fun init(tokenProvider: () -> String?) {
        this.tokenProvider = tokenProvider
    }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { tokenProvider() })
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** [rawServerUrl] es la dirección tal cual la introdujo o guardó el usuario. */
    fun api(rawServerUrl: String): SelfpotifyApi {
        val baseUrl = ServerUrl.baseUrl(rawServerUrl)
        cache?.let { if (it.first == baseUrl) return it.second }
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SelfpotifyApi::class.java)
        cache = baseUrl to api
        return api
    }
}
