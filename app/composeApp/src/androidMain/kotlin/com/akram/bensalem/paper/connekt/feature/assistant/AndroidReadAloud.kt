package com.akram.bensalem.paper.connekt.feature.assistant

import android.media.MediaPlayer
import com.akram.bensalem.paper.connekt.core.settings.requireAndroidApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual fun saveSpeechPlanJson(fileName: String, json: String): Boolean {
    val directory = File(requireAndroidApplicationContext().filesDir, "paperconnekt/speech-plans")
    check(directory.isDirectory || directory.mkdirs()) { "Could not create speech plan storage." }
    val safeName = safePathSegment(fileName.substringAfterLast('/').substringAfterLast('\\'))
    File(directory, safeName).writeText(json)
    return true
}

actual fun createTtsAudioPlayer(): TtsAudioPlayer = AndroidTtsAudioPlayer()

private class AndroidTtsAudioPlayer : TtsAudioPlayer {
    private var player: MediaPlayer? = null
    @Volatile
    private var playerPrepared = false
    private val audioCacheDir: File by lazy {
        File(requireAndroidApplicationContext().cacheDir, "paperconnekt/tts-audio").apply {
            check(isDirectory || mkdirs()) { "Could not create TTS audio cache." }
        }
    }

    override suspend fun preload(url: String) {
        withContext(Dispatchers.IO) {
            cachedAudioFile(url)
        }
    }

    override suspend fun play(url: String) {
        val audioFile = withContext(Dispatchers.IO) { cachedAudioFile(url) }
        suspendCancellableCoroutine { continuation ->
            val mediaPlayer = MediaPlayer()
            playerPrepared = false
            player?.release()
            player = mediaPlayer
            mediaPlayer.setOnPreparedListener {
                playerPrepared = true
                if (continuation.isActive) {
                    it.start()
                } else {
                    playerPrepared = false
                    it.release()
                    if (player === it) player = null
                }
            }
            mediaPlayer.setOnCompletionListener {
                playerPrepared = false
                if (continuation.isActive) continuation.resume(Unit)
                it.release()
                if (player === it) player = null
            }
            mediaPlayer.setOnErrorListener { source, what, extra ->
                playerPrepared = false
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        IllegalStateException("Audio playback failed (what=$what, extra=$extra)."),
                    )
                }
                source.release()
                if (player === source) player = null
                true
            }
            continuation.invokeOnCancellation {
                playerPrepared = false
                if (player === mediaPlayer) player = null
                mediaPlayer.release()
            }
            try {
                // MediaPlayer can fail while preparing public CDN WAV URLs because it probes
                // them with HTTP range requests. A local file also matches desktop playback,
                // which downloads the complete audio before decoding it.
                mediaPlayer.setDataSource(audioFile.absolutePath)
                mediaPlayer.prepareAsync()
            } catch (exception: Exception) {
                playerPrepared = false
                if (continuation.isActive) continuation.resumeWithException(exception)
                mediaPlayer.release()
                if (player === mediaPlayer) player = null
            }
        }
    }

    override fun pause() {
        if (!playerPrepared) return
        player?.let { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
            } catch (_: IllegalStateException) {
                // The completion callback may release the player while the UI is pausing it.
            }
        }
    }

    override fun resume() {
        if (!playerPrepared) return
        player?.let { mediaPlayer ->
            try {
                if (!mediaPlayer.isPlaying) mediaPlayer.start()
            } catch (_: IllegalStateException) {
                // The completion callback may release the player while the UI is resuming it.
            }
        }
    }

    override fun stop() {
        player?.let {
            player = null
            playerPrepared = false
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            it.release()
        }
    }

    override fun progress(): Float? = player?.let { mediaPlayer ->
        if (!playerPrepared) return@let null
        try {
            val duration = mediaPlayer.duration
            if (duration > 0) mediaPlayer.currentPosition.toFloat() / duration else null
        } catch (_: IllegalStateException) {
            null
        }
    }

    override fun positionSeconds(): Float? = player?.let { mediaPlayer ->
        if (!playerPrepared) return@let null
        try {
            mediaPlayer.currentPosition / 1000f
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun cachedAudioFile(url: String): File {
        val target = File(audioCacheDir, "${url.sha256()}.wav")
        if (target.isFile && target.length() > 44L) return target

        val temporary = File(audioCacheDir, "${target.name}.tmp")
        try {
            URL(url).openStream().use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            check(temporary.length() > 44L) { "The speech audio file is empty." }
            check(temporary.renameTo(target)) { "Could not store speech audio." }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
        return target
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
