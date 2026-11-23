package com.headunit.launcher

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.MediaPlayer
import java.io.File

// The spoken directions played by the app itself instead of by the speech engine (Settings › Sound & voice):
// - "Play the voice like the chimes": some head units lower the music for the app's own sounds (the chimes) but not for the
//   speech engine's voice. The voice is then read into a file and played the same way as the chimes.
// - "Voice comes out of › Phone speaker": the voice and the chimes stay on the phone while music plays over Bluetooth
//   (Android 9 and later; never with Android Auto, which has the car's speakers).
// Utterances played this way have ids starting "navf" (see speakNextSegment and the speech engine's listener in NavEngine).

/** Whether the voice and chimes go to the phone's own speaker (set, Android 9 or later, and no car screen connected). */
internal fun NavEngine.speakerOut(): Boolean =
    prefs.s(PreferenceKeys.VOICE_OUT, "auto") == "speaker" && android.os.Build.VERSION.SDK_INT >= 28 && !isCar && !RouteSync.carOn

/** Whether the voice is played by the app (read into a file first) rather than by the speech engine. */
internal fun NavEngine.selfPlayVoice(): Boolean = prefs.b(PreferenceKeys.VOICE_AS_CHIME, false) || speakerOut()

/** The phone's built-in loudspeaker, when there is one. */
internal fun NavEngine.phoneSpeaker(): AudioDeviceInfo? = try {
    val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    am.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
} catch (e: Exception) { softFail(e); null }

private fun NavEngine.voiceFile(id: String) = File(ctx.cacheDir, "utt_$id.wav")

/** Reads [text] into a file for utterance [id]; it is played when the speech engine says the file is done. */
internal fun NavEngine.synthToFile(t: android.speech.tts.TextToSpeech, text: String, id: String): Boolean {
    val f = voiceFile(id)
    return t.synthesizeToFile(text, null, f, id) != android.speech.tts.TextToSpeech.ERROR
}

/** The file for utterance [id] is ready: plays it (if it's still the one to say), then goes on to the next. */
internal fun NavEngine.playVoiceFile(id: String) {
    speechMain.post {
        val f = voiceFile(id)
        if (id != playingId) { f.delete(); return@post }
        try {
            val mp = MediaPlayer()
            voicePlayer = mp
            val like = prefs.b(PreferenceKeys.VOICE_AS_CHIME, false)
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(voiceUsage(voiceChan))
                .setContentType(if (like) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_SPEECH).build())
            if (android.os.Build.VERSION.SDK_INT >= 28 && speakerOut()) phoneSpeaker()?.let { mp.setPreferredDevice(it) }
            mp.setDataSource(f.path)
            fun done() {
                if (voicePlayer === mp) voicePlayer = null
                try { mp.release() } catch (e: Exception) { softFail(e) }
                f.delete()
                uttDone(id)
            }
            mp.setOnCompletionListener { done() }
            mp.setOnErrorListener { _, _, _ -> done(); true }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            softFail(e)
            voicePlayer = null
            f.delete()
            uttDone(id)
        }
    }
}

/** Stops whatever is being said: the speech engine and the app's own player. */
internal fun NavEngine.stopVoice() {
    try { tts?.stop() } catch (e: Exception) { softFail(e) }
    voicePlayer?.let { mp -> voicePlayer = null; try { mp.stop(); mp.release() } catch (e: Exception) { softFail(e) } }
}
