package davila.anton.selfpotify.ui

/** Destinos del grafo de navegación externo de Compose (flujo de acceso + app principal). */
object Route {
    const val SERVER = "server"
    const val AUTH = "auth"
    /** Contenedor principal logueado: bottom nav + mini-player. */
    const val HOME = "home"
    /** Reproductor a pantalla completa (se abre sobre el contenedor principal). */
    const val PLAYER = "player"
    const val OFFLINE = "offline"
}
