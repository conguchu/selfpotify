package davila.anton.selfpotify.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** DataStore único del proceso (definido a nivel de Context para evitar instancias duplicadas). */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "selfpotify_session")

/**
 * Persistencia de la sesión (CLAUDE.md §5):
 * - la dirección del servidor se guarda "para siempre" hasta que el usuario cambie de servidor;
 * - el JWT se guarda asociado al servidor que lo emitió, para no reutilizarlo en otro servidor.
 */
class SessionStore(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val JWT = stringPreferencesKey("jwt_token")
        val JWT_SERVER = stringPreferencesKey("jwt_server_url")
        val USERNAME = stringPreferencesKey("username")
        // Tokens de color de marca del servidor (branding.colors) serializados a JSON.
        val BRANDING_COLORS = stringPreferencesKey("branding_colors")
    }

    data class Session(
        val serverUrl: String?,
        val token: String?,
        val tokenServer: String?,
        val username: String?,
    ) {
        val hasServer: Boolean get() = !serverUrl.isNullOrBlank()

        /** Solo hay sesión si el JWT existe y fue emitido por el servidor actual. */
        val isLoggedIn: Boolean
            get() = hasServer && !token.isNullOrBlank() && tokenServer == serverUrl
    }

    val session: Flow<Session> = store.data.map { p ->
        Session(
            serverUrl = p[Keys.SERVER_URL],
            token = p[Keys.JWT],
            tokenServer = p[Keys.JWT_SERVER],
            username = p[Keys.USERNAME],
        )
    }

    suspend fun current(): Session = session.first()

    /**
     * Paleta de marca del servidor (mapa de tokens CSS). Pertenece al servidor, no a la
     * sesión: sobrevive al logout y solo se borra al cambiar de servidor ([clearAll]).
     */
    val brandingColors: Flow<Map<String, String>?> = store.data.map { p ->
        p[Keys.BRANDING_COLORS]?.let { json ->
            runCatching { gson.fromJson<Map<String, String>>(json, COLORS_TYPE) }.getOrNull()
        }
    }

    suspend fun currentBrandingColors(): Map<String, String>? = brandingColors.first()

    suspend fun saveServer(canonicalUrl: String) {
        store.edit { it[Keys.SERVER_URL] = canonicalUrl }
    }

    /** Persiste (o borra, si es null/vacío) los tokens de color del servidor. */
    suspend fun saveBrandingColors(colors: Map<String, String>?) {
        store.edit {
            if (colors.isNullOrEmpty()) it.remove(Keys.BRANDING_COLORS)
            else it[Keys.BRANDING_COLORS] = gson.toJson(colors)
        }
    }

    /** Guarda el JWT atándolo al servidor que lo emitió. */
    suspend fun saveSession(token: String, serverUrl: String, username: String) {
        store.edit {
            it[Keys.JWT] = token
            it[Keys.JWT_SERVER] = serverUrl
            it[Keys.USERNAME] = username
        }
    }

    /** Logout: borra solo el JWT y el usuario; conserva el servidor. */
    suspend fun clearSession() {
        store.edit {
            it.remove(Keys.JWT)
            it.remove(Keys.JWT_SERVER)
            it.remove(Keys.USERNAME)
        }
    }

    /** Cambiar de servidor: borra el servidor, su JWT y su paleta de marca. */
    suspend fun clearAll() {
        store.edit { it.clear() }
    }

    private companion object {
        val gson = Gson()
        val COLORS_TYPE = object : TypeToken<Map<String, String>>() {}.type
    }
}
