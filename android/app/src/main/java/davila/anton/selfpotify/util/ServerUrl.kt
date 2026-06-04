package davila.anton.selfpotify.util

/**
 * Normalización de la dirección del servidor introducida por el usuario.
 *
 * El usuario escribe la dirección de nginx (el mismo host que la web, `:80` por
 * defecto), p. ej. `192.168.1.10` o `http://mi-servidor/`.
 * - [canonical] devuelve la forma estable que se persiste y con la que se asocia el JWT
 *   (con esquema, sin barra final).
 * - [baseUrl] devuelve la forma que exige Retrofit (con barra final).
 */
object ServerUrl {

    /** Forma canónica: garantiza esquema http(s) y elimina barras finales. */
    fun canonical(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        if (!s.startsWith("http://", ignoreCase = true) &&
            !s.startsWith("https://", ignoreCase = true)
        ) {
            s = "http://$s"
        }
        return s.trimEnd('/')
    }

    /** Base URL para Retrofit (siempre termina en `/`). */
    fun baseUrl(raw: String): String = canonical(raw) + "/"

    /**
     * Resuelve una ruta de asset del servidor (p. ej. `branding.logoUrl = /assets/logo.png`)
     * a una URL absoluta contra [rawServer]. Si la ruta ya es absoluta (http/https) se
     * devuelve tal cual; si servidor o ruta están vacíos devuelve `null`.
     */
    fun asset(rawServer: String?, path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://", ignoreCase = true) ||
            path.startsWith("https://", ignoreCase = true)
        ) {
            return path
        }
        if (rawServer.isNullOrBlank()) return null
        return canonical(rawServer) + "/" + path.trimStart('/')
    }
}
