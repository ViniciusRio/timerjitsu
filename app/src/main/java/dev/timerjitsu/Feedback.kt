package dev.timerjitsu

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** All sounds are synthesized here; the app ships no audio files. */
private object Synth {
    const val RATE = 44100

    private fun render(seconds: Double, fn: (Double) -> Double): ShortArray {
        val n = (seconds * RATE).toInt()
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val fade = minOf(1.0, (n - i) / (RATE * 0.005))
            (fn(t).coerceIn(-1.0, 1.0) * fade * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
    }

    private fun tone(f: Double, t: Double) = sin(2 * PI * f * t)

    fun cue(c: Cue): ShortArray = when (c) {
        Cue.BEEP -> render(0.35) { t -> tone(1200.0, t) * exp(-t * 3) }
        Cue.DOUBLE_BEEP -> render(0.42) { t -> if (t < 0.16 || t > 0.24) tone(1400.0, t) * 0.9 else 0.0 }
        Cue.BELL -> render(1.4) { t ->
            (tone(880.0, t) + 0.6 * tone(1760.0 * 1.003, t) + 0.3 * tone(2640.0 * 1.007, t)) / 1.9 * exp(-t * 3)
        }
        Cue.GONG -> render(2.2) { t ->
            (tone(196.0, t) + 0.7 * tone(311.0, t) + 0.5 * tone(523.0, t) + 0.25 * tone(830.0, t)) / 2.45 * exp(-t * 1.8)
        }
        Cue.WHISTLE -> render(0.7) { t -> tone(2700.0 + 90 * sin(2 * PI * 28 * t), t) * minOf(1.0, t * 40) }
        Cue.BUZZER -> render(0.8) { t ->
            val saw = (1..8).sumOf { k -> tone(170.0 * k, t) / k }
            saw * 0.55
        }
        Cue.SOFT -> render(1.0) { t -> (tone(528.0, t) + 0.4 * tone(792.0, t)) / 1.4 * minOf(1.0, t * 12) * exp(-t * 3.5) }
    }

    fun pip(): ShortArray = render(0.12) { t -> tone(1000.0, t) * exp(-t * 12) }
    fun tick(): ShortArray = render(0.03) { t -> tone(2200.0, t) * exp(-t * 140) * 0.5 }
}

class Feedback(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs).build()
    private val tracks = HashMap<String, AudioTrack>()
    private val releaseFocus = Runnable { audio.abandonAudioFocusRequest(focus) }
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) app.getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") app.getSystemService(Vibrator::class.java)

    private fun track(key: String, pcm: () -> ShortArray): AudioTrack = tracks.getOrPut(key) {
        val data = pcm()
        AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(AudioFormat.Builder().setSampleRate(Synth.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build().also { it.write(data, 0, data.size) }
    }

    private fun play(key: String, volume: Float, focusMs: Long, pcm: () -> ShortArray) {
        val t = track(key, pcm)
        audio.requestAudioFocus(focus)
        handler.removeCallbacks(releaseFocus)
        handler.postDelayed(releaseFocus, focusMs)
        runCatching {
            t.pause(); t.flush(); t.stop()
            t.reloadStaticData()
            t.setVolume(volume)
            t.play()
        }
    }

    fun cue(c: Cue, volume: Float) = play(c.name, volume, 2500) { Synth.cue(c) }
    fun pip(volume: Float) = play("pip", volume, 600) { Synth.pip() }
    fun tick(volume: Float) = play("tick", volume * 0.6f, 300) { Synth.tick() }

    fun speak(text: String) {
        val engine = tts ?: TextToSpeech(app) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
            }
        }.also { tts = it }
        if (ttsReady) engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    fun vibrate(ms: Long) {
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun blinkTorch() {
        val cam = app.getSystemService(CameraManager::class.java)
        val id = runCatching {
            cam.cameraIdList.firstOrNull { cam.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull() ?: return
        runCatching {
            cam.setTorchMode(id, true)
            handler.postDelayed({ runCatching { cam.setTorchMode(id, false) } }, 250)
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        audio.abandonAudioFocusRequest(focus)
        tracks.values.forEach { it.release() }
        tracks.clear()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
