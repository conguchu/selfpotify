package davila.anton.selfpotify.data.network

import davila.anton.selfpotify.data.model.JwtResponse
import davila.anton.selfpotify.data.model.LoginRequest
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.PublicConfig
import davila.anton.selfpotify.data.model.SearchResponseDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.StreamTokenResponse
import davila.anton.selfpotify.data.model.Top10GenreSongsDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Interfaz Retrofit con los endpoints que consume la app. */
interface SelfpotifyApi {

    /** Público. Se usa para validar que la dirección es un servidor Selfpotify. */
    @GET("api/config/public")
    suspend fun getPublicConfig(): PublicConfig

    /** Público. Devuelve el JWT. */
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): JwtResponse

    /** Público. Devuelve texto plano ("User registered successfully!"). */
    @POST("api/auth/signup")
    suspend fun signup(@Body body: LoginRequest): ResponseBody

    // --- Música / descubrimiento (requieren JWT vía AuthInterceptor) ---

    /** Descubrimientos diarios del usuario (hasta 9 canciones, estables por día). */
    @GET("api/feed/daily-discoveries")
    suspend fun dailyDiscoveries(): List<SongDTO>

    /** Canciones aleatorias del catálogo (scroll infinito de Descubrir). */
    @GET("api/songs/random")
    suspend fun randomSongs(@Query("count") count: Int = 10): List<SongDTO>

    /** Artistas recomendados del home, personalizados por usuario (carrusel de Descubrir). */
    @GET("api/feed")
    suspend fun homeFeed(): List<ArtistDTO>

    /** Géneros escuchados más recientemente (máx. 10, del más reciente al más antiguo). */
    @GET("api/feed/genres")
    suspend fun recentGenres(): List<String>

    /** Top 10 canciones de un género por escuchas. El backend usa @RequestParam `genre`. */
    @GET("api/songs/top")
    suspend fun genreTopSongs(@Query("genre") genre: String): Top10GenreSongsDTO

    /** Emite un stream token de corta vida para construir las URLs de `/api/listen/{id}`. */
    @POST("api/listen/token")
    suspend fun streamToken(): StreamTokenResponse

    // --- Búsqueda ---

    /**
     * Búsqueda global. En modo `all` (default) devuelve hasta 5 elementos por categoría, pensado
     * como vista previa multi-categoría. Si [q] está en blanco el backend devuelve la respuesta con
     * todas las categorías a 0.
     */
    @GET("api/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("type") type: String = "all",
    ): SearchResponseDTO

    // --- Playlists ---

    /** Playlists del usuario autenticado (privadas y públicas). */
    @GET("api/playlists/my")
    suspend fun myPlaylists(): List<PlaylistDTO>

    /** Añade una canción a una playlist (creador o colaborador). Idempotente. */
    @POST("api/playlists/{id}/songs/{songId}")
    suspend fun addSongToPlaylist(
        @Path("id") playlistId: Long,
        @Path("songId") songId: Long,
    ): PlaylistDTO

    // --- Perfil ---

    /** Vista pública del usuario autenticado. */
    @GET("api/me")
    suspend fun me(): UserSummaryDTO
}
