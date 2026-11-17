package com.headunit.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The notification listener that lets the app see what plays (the song, the artist, the artwork). It reads nothing from the
 * notifications themselves; the access is only what Android needs to show the media sessions.
 */
class MediaNotifListener : NotificationListenerService()

/**
 * Media on the head unit, from any app: back, play / pause and next beside the driving screen while something plays. Without
 * notification access the bar still works (it knows music is playing, and sends the media keys); the song and artwork need the access.
 */
object MediaHub {
    data class Now(val playing: Boolean = false, val title: String = "", val artist: String = "", val art: Bitmap? = null)

    var now by mutableStateOf(Now())
        private set

    /** Whether Horizon Nav has notification access (Settings › Screen & sizes › While driving › Song and artwork). */
    fun hasAccess(ctx: Context): Boolean = com.headunit.launcher.BuildConfig.MEDIA_ACCESS && try {
        android.provider.Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")?.contains(ctx.packageName) == true
    } catch (e: Exception) { false }

    /** Looks at what plays now (called about once a second while the driving screen shows). */
    fun refresh(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        var playing = try { am?.isMusicActive == true } catch (e: Exception) { false }
        var title = ""; var artist = ""; var art: Bitmap? = null
        try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = msm.getActiveSessions(ComponentName(ctx, MediaNotifListener::class.java))
            val s = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
            if (s != null) {
                val md = s.metadata
                title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                if (s.playbackState?.state == PlaybackState.STATE_PLAYING) playing = true
            }
        } catch (e: SecurityException) { } catch (e: Exception) { }
        now = Now(playing, title, artist, art)
    }

    /** "prev", "next" or "toggle" (play / pause): the playing app's own controls when Android lets us, else the media keys. */
    fun control(ctx: Context, action: String) {
        try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = msm.getActiveSessions(ComponentName(ctx, MediaNotifListener::class.java))
            val s = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
            if (s != null) {
                val t = s.transportControls
                val playingNow = s.playbackState?.state == PlaybackState.STATE_PLAYING
                when (action) {
                    "prev" -> t.skipToPrevious()
                    "next" -> t.skipToNext()
                    else -> if (playingNow) t.pause() else t.play()
                }
                return
            }
        } catch (e: SecurityException) { } catch (e: Exception) { }
        // no access (or no session): the media keys, which the head unit's current player takes
        val code = when (action) { "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS; "next" -> KeyEvent.KEYCODE_MEDIA_NEXT; else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE }
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        } catch (e: Exception) { }
    }
}
