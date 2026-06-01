package davila.anton.selfpotify.data.network

import davila.anton.selfpotify.data.model.JwtResponse
import davila.anton.selfpotify.data.model.LoginRequest
import davila.anton.selfpotify.data.model.PublicConfig
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Interfaz Retrofit con los endpoints necesarios para el flujo de login. */
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
}
