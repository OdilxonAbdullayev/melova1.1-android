package uz.melova.app.playback

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Session remains alive for headset controls, lockscreen controls and background playback. */
class MelovaPlaybackService:MediaSessionService() {
    private var mediaSession:MediaSession?=null
    override fun onCreate() {
        super.onCreate()
        val player=ExoPlayer.Builder(this).build().apply {
            playWhenReady=false
        }
        mediaSession=MediaSession.Builder(this,player).build()
    }
    override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=mediaSession
    override fun onTaskRemoved(rootIntent:Intent?) {
        val p=mediaSession?.player
        if(p==null || !p.playWhenReady || p.mediaItemCount==0) stopSelf()
    }
    override fun onDestroy() {
        mediaSession?.run { player.release();release() }
        mediaSession=null
        super.onDestroy()
    }
}
