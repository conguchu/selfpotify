package davila.anton.selfpotify.data.network

import davila.anton.selfpotify.data.model.JwtResponse
import davila.anton.selfpotify.data.model.LoginRequest
import davila.anton.selfpotify.data.model.PlaylistDTO
import davila.anton.selfpotify.data.model.PlaylistInput
import davila.anton.selfpotify.data.model.ProfileUpdateRequest
import davila.anton.selfpotify.data.model.PublicConfig
import davila.anton.selfpotify.data.model.ShareLinkResponse
import davila.anton.selfpotify.data.model.SearchResponseDTO
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.model.AlbumDTO
import davila.anton.selfpotify.data.model.ArtistDTO
import davila.anton.selfpotify.data.model.StreamTokenResponse
import davila.anton.selfpotify.data.model.Top10ArtistTracksDTO
import davila.anton.selfpotify.data.model.Top10GenreSongsDTO
import davila.anton.selfpotify.data.model.UserSummaryDTO
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    // --- Detalle (artista / álbum / playlist / usuario) ---

    /** Una canción por id (para resolver las `songIds` de álbumes y playlists). */
    @GET("api/songs/{id}")
    suspend fun getSong(@Path("id") id: Long): SongDTO

    /** Artista por id. */
    @GET("api/artists/{id}")
    suspend fun getArtist(@Path("id") id: Long): ArtistDTO

    /** Top 10 canciones del artista por escuchas. */
    @GET("api/artists/{id}/top-10-tracks")
    suspend fun artistTopTracks(@Path("id") id: Long): Top10ArtistTracksDTO

    /** Álbum por id (trae `songIds`). */
    @GET("api/albums/{id}")
    suspend fun getAlbum(@Path("id") id: Long): AlbumDTO

    /** Vista pública de un usuario cualquiera por id. */
    @GET("api/users/{id}/public")
    suspend fun getUserPublic(@Path("id") id: Long): UserSummaryDTO

    /** Playlists públicas de un usuario cualquiera. */
    @GET("api/playlists/user/{userId}")
    suspend fun userPlaylists(@Path("userId") userId: Long): List<PlaylistDTO>

    // --- Playlists ---

    /** Playlists del usuario autenticado (privadas y públicas). */
    @GET("api/playlists/my")
    suspend fun myPlaylists(): List<PlaylistDTO>

    /** Playlists compartidas conmigo (soy colaborador, no creador). */
    @GET("api/playlists/shared")
    suspend fun sharedPlaylists(): List<PlaylistDTO>

    /** Una playlist por id (trae `songIds`); incluye públicas y propias. */
    @GET("api/playlists/{id}")
    suspend fun getPlaylist(@Path("id") id: Long): PlaylistDTO

    /** Crea una playlist. Devuelve la creada (el creador queda como colaborador). */
    @POST("api/playlists")
    suspend fun createPlaylist(@Body body: PlaylistInput): PlaylistDTO

    /** Edita los metadatos de una playlist (solo el creador). */
    @PUT("api/playlists/{id}")
    suspend fun updatePlaylist(@Path("id") id: Long, @Body body: PlaylistInput): PlaylistDTO

    /** Borra una playlist (creador o admin). */
    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Long): Response<Unit>

    /** Sube/reemplaza la carátula de una playlist (solo el creador). Multipart, campo `file`. */
    @Multipart
    @POST("api/playlists/{id}/cover")
    suspend fun uploadPlaylistCover(
        @Path("id") id: Long,
        @Part file: MultipartBody.Part,
    ): PlaylistDTO

    /** Genera un magic link de un solo uso para compartir la playlist (solo el creador). */
    @POST("api/playlists/{id}/share")
    suspend fun sharePlaylist(@Path("id") id: Long): ShareLinkResponse

    /**
     * Canjea un magic link: añade al usuario autenticado como colaborador y devuelve la playlist.
     * Mismo endpoint que usa la web; lo invoca el handoff del deep link `selfpotify://`.
     */
    @POST("api/playlists/share/{token}")
    suspend fun redeemShareLink(@Path("token") token: String): PlaylistDTO

    /** Colaboradores actuales de la playlist. */
    @GET("api/playlists/{id}/collaborators")
    suspend fun playlistCollaborators(@Path("id") id: Long): List<UserSummaryDTO>

    /** Quita a un colaborador de la playlist (solo el creador). */
    @DELETE("api/playlists/{id}/collaborators/{userId}")
    suspend fun removeCollaborator(
        @Path("id") id: Long,
        @Path("userId") userId: Long,
    ): Response<Unit>

    /** Añade una canción a una playlist (creador o colaborador). Idempotente. */
    @POST("api/playlists/{id}/songs/{songId}")
    suspend fun addSongToPlaylist(
        @Path("id") playlistId: Long,
        @Path("songId") songId: Long,
    ): PlaylistDTO

    /** Quita una canción de una playlist (creador o colaborador). Devuelve la playlist actualizada. */
    @DELETE("api/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") playlistId: Long,
        @Path("songId") songId: Long,
    ): PlaylistDTO

    // --- Perfil ---

    /** Vista pública del usuario autenticado. */
    @GET("api/me")
    suspend fun me(): UserSummaryDTO

    /** Edita el nombre visible del usuario autenticado (`name` nulo/vacío lo borra). */
    @PUT("api/me/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): UserSummaryDTO

    /** Sube/reemplaza la foto del usuario autenticado. Multipart, campo `file`. */
    @Multipart
    @POST("api/me/profile/picture")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserSummaryDTO

    /** Borra la foto del usuario autenticado (deja `avatarUrl` a `null`). */
    @DELETE("api/me/profile/picture")
    suspend fun deleteAvatar(): UserSummaryDTO

    // --- Seguimiento entre usuarios ---

    /** Seguidores de un usuario (más recientes primero). */
    @GET("api/users/{id}/followers")
    suspend fun followers(@Path("id") id: Long): List<UserSummaryDTO>

    /** Usuarios a los que sigue un usuario (más recientes primero). */
    @GET("api/users/{id}/following")
    suspend fun following(@Path("id") id: Long): List<UserSummaryDTO>

    /** Sigue a un usuario. Idempotente. Devuelve el `UserSummaryDTO` con los contadores al día. */
    @POST("api/users/{id}/follow")
    suspend fun follow(@Path("id") id: Long): UserSummaryDTO

    /** Deja de seguir a un usuario. Idempotente. Devuelve el `UserSummaryDTO` actualizado. */
    @DELETE("api/users/{id}/follow")
    suspend fun unfollow(@Path("id") id: Long): UserSummaryDTO
}
