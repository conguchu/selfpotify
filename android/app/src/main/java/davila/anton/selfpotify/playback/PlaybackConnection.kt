package davila.anton.selfpotify.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.data.model.SongDTO
import davila.anton.selfpotify.data.repository.StreamTokenRepository
import davila.anton.selfpotify.util.ServerUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado del reproductor expuesto a la UI (snapshot inmutable para Compose). */
data class PlayerState(
    val hasItem: Boolean = false,
    val isPlaying: Boolean = false,
    val songId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val artistId: Long? = null,
    val artworkUrl: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isLooping: Boolean = false,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/**
 * Puente entre la UI y el [PlaybackService]. Se conecta vía un [MediaController] y expone el
 * estado del player como [StateFlow] para que los ViewModels lo colecten (MVVM, CLAUDE.md §2).
 *
 * Singleton de proceso: una sola conexión compartida por mini-player y reproductor completo.
 * Se inicializa una vez al arrancar ([init]).
 */
object PlaybackConnection {

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private lateinit var appContext: Context
    private lateinit var streamTokens: StreamTokenRepository
    private lateinit var session: SessionStore
    private var initialized = false

    /** Conecta con el servicio. Idempotente: solo la primera llamada surte efecto. */
    fun init(context: Context, streamTokens: StreamTokenRepository, session: SessionStore) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext
        this.streamTokens = streamTokens
        this.session = session

        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(PlayerListener) }
            syncState()
            startPositionUpdates()
        }, ContextCompat.getMainExecutor(appContext))
    }

    /**
     * Carga [songs] como cola y empieza a reproducir desde [startIndex]. Pide un único stream
     * token y construye con él las URLs de todas las canciones (`/api/listen/{id}?st=`).
     */
    suspend fun playFrom(songs: List<SongDTO>, startIndex: Int) {
        val ctrl = controller ?: return
        if (songs.isEmpty()) return
        val token = streamTokens.streamToken().getOrNull() ?: return
        val server = session.current().serverUrl ?: return
        val items = songs.map { mediaItem(it, server, token) }
        ctrl.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0)
        ctrl.prepare()
        ctrl.play()
    }

    /**
     * Añade [songs] al final de la cola actual sin interrumpir la reproducción. Lo usa el
     * carrusel diario de Descubrir para que, al llegar a la última canción, sigan sonando las
     * siguientes en lugar de detenerse al final (ver "Pantalla Descubrir" en el README).
     */
    suspend fun appendSongs(songs: List<SongDTO>) {
        val ctrl = controller ?: return
        if (songs.isEmpty()) return
        val token = streamTokens.streamToken().getOrNull() ?: return
        val server = session.current().serverUrl ?: return
        ctrl.addMediaItems(songs.map { mediaItem(it, server, token) })
    }

    /** Construye el [MediaItem] de una canción con la URL de streaming firmada (`?st=`). */
    private fun mediaItem(song: SongDTO, server: String, token: String): MediaItem {
        val url = "${ServerUrl.canonical(server)}/api/listen/${song.id}?st=$token"
        // El id del artista principal viaja en los extras para poder abrir su detalle desde el
        // reproductor (la metadata estándar de Media3 solo guarda el nombre, no el id).
        val extras = song.artistIds?.firstOrNull()?.let { Bundle().apply { putLong(KEY_ARTIST_ID, it) } }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title.orEmpty())
                    .setArtist(song.artistsLabel)
                    .setArtworkUri(ServerUrl.asset(server, song.pictureUrl)?.let(Uri::parse))
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    fun togglePlay() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun next() = controller?.seekToNextMediaItem() ?: Unit

    fun previous() = controller?.seekToPreviousMediaItem() ?: Unit

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    /** Alterna entre repetir la canción actual (loop) y no repetir. */
    fun toggleRepeat() {
        val ctrl = controller ?: return
        ctrl.repeatMode =
            if (ctrl.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF
            else Player.REPEAT_MODE_ONE
    }

    /** Clave del id del artista principal en los extras del [MediaMetadata]. */
    private const val KEY_ARTIST_ID = "selfpotify.artistId"

    private object PlayerListener : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    /** Vuelca el estado del controller al [StateFlow], preservando la posición del poller. */
    private fun syncState() {
        val ctrl = controller ?: return
        val meta = ctrl.mediaMetadata
        _state.value = _state.value.copy(
            hasItem = ctrl.mediaItemCount > 0,
            isPlaying = ctrl.isPlaying,
            songId = ctrl.currentMediaItem?.mediaId?.toLongOrNull(),
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            artistId = meta.extras?.takeIf { it.containsKey(KEY_ARTIST_ID) }?.getLong(KEY_ARTIST_ID),
            artworkUrl = meta.artworkUri?.toString(),
            durationMs = ctrl.duration.coerceAtLeast(0),
            positionMs = ctrl.currentPosition.coerceAtLeast(0),
            isLooping = ctrl.repeatMode == Player.REPEAT_MODE_ONE,
            hasNext = ctrl.hasNextMediaItem(),
            hasPrevious = ctrl.hasPreviousMediaItem(),
        )
    }

    /** Refresca la posición/duración mientras suena, para mover el slider del reproductor. */
    private fun startPositionUpdates() {
        scope.launch {
            while (true) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = ctrl.currentPosition.coerceAtLeast(0),
                        durationMs = ctrl.duration.coerceAtLeast(0),
                    )
                }
                delay(500)
            }
        }
    }
}
