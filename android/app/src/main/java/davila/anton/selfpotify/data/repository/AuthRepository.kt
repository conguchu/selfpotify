package davila.anton.selfpotify.data.repository

import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.JwtResponse
import davila.anton.selfpotify.data.model.LoginRequest
import davila.anton.selfpotify.data.model.PublicConfig
import davila.anton.selfpotify.data.network.ApiProvider
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /** Guarda la paleta de marca del servidor (tokens de `branding.colors`). */
    suspend fun saveBranding(colors: Map<String, String>?) {
        session.saveBrandingColors(colors)
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
     * Refresca la paleta de marca desde la config pública del servidor al iniciar sesión,
     * por si cambió desde la validación inicial. Best-effort: un fallo no aborta el login.
     */
    private suspend fun refreshBranding(server: String) {
        runCatching { ApiProvider.api(server).getPublicConfig().branding?.colors }
            .getOrNull()
            ?.let { session.saveBrandingColors(it) }
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
