package davila.anton.selfpotify.util

/**
 * Normalización de la dirección del servidor introducida por el usuario.
 *
 * El usuario escribe algo como `192.168.1.10:8080` o `http://mi-servidor:8080/`.
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
}
