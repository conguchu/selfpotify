package davila.anton.selfpotify.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Servicio de reproducción en primer plano (README, sección Android → reproductor).
 *
 * Aloja un único [ExoPlayer] envuelto en una [MediaSession]. Al correr como
 * [MediaSessionService], Media3 publica automáticamente la notificación multimedia (con sus
 * controles) y la sesión es visible en la pantalla de bloqueo, de modo que la música sobrevive
 * cuando la app pasa a segundo plano. La UI no habla con este servicio directamente: se conecta
 * a través de un `MediaController` (ver [PlaybackConnection]).
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // Gestiona el foco de audio (pausa al recibir llamadas, baja volumen con otras apps).
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /** Si el usuario descarta la tarea sin nada sonando, paramos el servicio. */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
