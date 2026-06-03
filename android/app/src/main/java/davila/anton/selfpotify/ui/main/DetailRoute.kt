package davila.anton.selfpotify.ui.main

/**
 * Destinos de detalle dentro del `NavHost` anidado de [MainScreen]. Viven en el grafo de las
 * pestañas (no en el externo) para que la barra inferior y el mini-player sigan visibles al abrir
 * un artista, álbum, playlist o usuario, igual que en Spotify.
 */
object DetailRoute {
    const val ARG_ID = "id"

    const val ARTIST = "artist/{$ARG_ID}"
    const val ALBUM = "album/{$ARG_ID}"
    const val PLAYLIST = "playlist/{$ARG_ID}"
    const val USER = "user/{$ARG_ID}"
    const val FOLLOWERS = "user/{$ARG_ID}/followers"
    const val FOLLOWING = "user/{$ARG_ID}/following"

    fun artist(id: Long) = "artist/$id"
    fun album(id: Long) = "album/$id"
    fun playlist(id: Long) = "playlist/$id"
    fun user(id: Long) = "user/$id"
    fun followers(id: Long) = "user/$id/followers"
    fun following(id: Long) = "user/$id/following"
}
