package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.JwtResponse
import davila.anton.selfpotify.data.model.LoginRequest
import davila.anton.selfpotify.data.model.PublicConfig
import davila.anton.selfpotify.data.network.ApiProvider
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/** Resultado de verificar la sesión contra el servidor (ver [AuthRepository.verifySession]). */
enum class SessionStatus {
    /** El JWT es válido: el servidor respondió a una petición autenticada. */
    VALID,

    /** El servidor rechazó el JWT (401/403): expiró o ya no es válido → hay que cerrar sesión. */
    EXPIRED,

    /** El servidor no responde (sin red o caído): no se puede afirmar nada del JWT. */
    OFFLINE,
}

/**
 * Única fuente de verdad para servidor + autenticación (CLAUDE.md §2).
 * Devuelve [Result] para propagar errores sin lanzar excepciones a la UI.
 */
class AuthRepository(private val session: SessionStore) {

    /**
     * Comprueba que [rawUrl] es realmente un servidor Selfpotify llamando a su
     * config pública. No persiste nada: solo valida.
     */
    suspend fun validateServer(rawUrl: String): Result<PublicConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val config = ApiProvider.api(ServerUrl.canonical(rawUrl)).getPublicConfig()
            // Heurística anti falso-positivo: un servidor Selfpotify siempre devuelve branding.appName.
            if (config.branding?.appName.isNullOrBlank()) {
                throw NotSelfpotifyServerException()
            }
            config
        }
    }

    /** Persiste la dirección del servidor en su forma canónica. */
    suspend fun saveServer(rawUrl: String) {
        session.saveServer(ServerUrl.canonical(rawUrl))
    }

    /** Guarda la marca del servidor: paleta (`branding.colors`) y logo (`branding.logoUrl`). */
    suspend fun saveBranding(colors: Map<String, String>?, logoUrl: String?) {
        session.saveBrandingColors(colors)
        session.saveBrandingLogoUrl(logoUrl)
    }

    /** Inicia sesión contra el servidor guardado y persiste el JWT asociado a él. */
    suspend fun login(username: String, password: String): Result<JwtResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val server = currentServer()
                val resp = ApiProvider.api(server).login(LoginRequest(username, password))
                session.saveSession(resp.token, server, resp.username)
                refreshBranding(server)
                resp
            }
        }

    /** Registra el usuario y, si tiene éxito, inicia sesión automáticamente. */
    suspend fun register(username: String, password: String): Result<JwtResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val server = currentServer()
                ApiProvider.api(server).signup(LoginRequest(username, password)).close()
                val resp = ApiProvider.api(server).login(LoginRequest(username, password))
                session.saveSession(resp.token, server, resp.username)
                refreshBranding(server)
                resp
            }
        }

    /**
     * Refresca la marca (paleta + logo) desde la config pública del servidor al iniciar
     * sesión, por si cambió desde la validación inicial. Best-effort: un fallo no aborta el
     * login (y se conserva la marca ya persistida).
     */
    private suspend fun refreshBranding(server: String) {
        runCatching { ApiProvider.api(server).getPublicConfig().branding }
            .getOrNull()
            ?.let {
                session.saveBrandingColors(it.colors)
                session.saveBrandingLogoUrl(it.logoUrl)
            }
    }

    /**
     * Comprueba (best-effort) si el servidor actual responde. Se usa estando ya logueado
     * para detectar la pérdida de conexión y mostrar la pantalla de sin-conexión.
     * Reutiliza el endpoint público, que no requiere JWT.
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        val server = session.current().serverUrl ?: return@withContext false
        runCatching { ApiProvider.api(server).getPublicConfig() }.isSuccess
    }

    /**
     * Verifica que el JWT guardado sigue siendo válido llamando a un endpoint autenticado
     * (`GET /api/me`). Se usa al entrar al contenedor principal para cerrar sesión
     * automáticamente si el token expiró o el servidor lo rechaza.
     *
     * Distingue tres casos: token válido, token rechazado por el servidor (401/403 → [EXPIRED])
     * y servidor inaccesible (sin red → [OFFLINE], se conserva la sesión para reintentar). Otros
     * errores HTTP del servidor no invalidan el token: se consideran [VALID].
     */
    suspend fun verifySession(): SessionStatus = withContext(Dispatchers.IO) {
        val current = session.current()
        if (!current.isLoggedIn || current.serverUrl == null) return@withContext SessionStatus.EXPIRED
        try {
            ApiProvider.api(current.serverUrl).me()
            SessionStatus.VALID
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) SessionStatus.EXPIRED else SessionStatus.VALID
        } catch (e: IOException) {
            SessionStatus.OFFLINE
        }
    }

    /** Logout: borra el JWT, conserva el servidor. */
    suspend fun logout() = session.clearSession()

    /** Cambiar de servidor: borra servidor + JWT asociados. */
    suspend fun forgetServer() = session.clearAll()

    private suspend fun currentServer(): String =
        session.current().serverUrl ?: throw IllegalStateException("No server configured")
}

/** La dirección respondió pero no es un servidor Selfpotify válido. */
class NotSelfpotifyServerException : Exception()
