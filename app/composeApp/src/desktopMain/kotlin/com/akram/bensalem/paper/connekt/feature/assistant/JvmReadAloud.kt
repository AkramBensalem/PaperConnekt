package com.akram.bensalem.paper.connekt.feature.assistant

import io.github.aakira.napier.Napier
import java.io.ByteArrayInputStream
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal fun createJvmTtsAudioPlayer(): TtsAudioPlayer = DesktopTtsAudioPlayer()

/**
 * Streams decoded PCM into a [SourceDataLine] and waits with `drain()` until the sound card has really
 * played every frame. `Clip` was not reliable here: on PulseAudio/ALSA its frame position advances in
 * one-second buffer steps, so short phrases were closed while their tail was still buffered.
 */
private class DesktopTtsAudioPlayer : TtsAudioPlayer {
    private val lock = Any()
    private var line: SourceDataLine? = null
    private var totalFrames = 0L
    private var paused = false
    private var stopped = false

    private val downloads = LinkedHashMap<String, ByteArray>(8, 0.75f, true)

    private fun bytes(url: String): ByteArray {
        synchronized(downloads) { downloads[url] }?.let { return it }
        val loaded = URL(url).openStream().use { it.readBytes() }
        synchronized(downloads) {
            downloads[url] = loaded
            while (downloads.size > 8) downloads.remove(downloads.keys.first())
        }
        return loaded
    }

    override suspend fun preload(url: String) {
        withContext(Dispatchers.IO) { bytes(url) }
    }

    override suspend fun play(url: String) = withContext(Dispatchers.IO) {
        val audio = bytes(url)
        stop()
        val stream = AudioSystem.getAudioInputStream(ByteArrayInputStream(audio))
        val format: AudioFormat = stream.format
        val pcm = stream.use { it.readBytes() }
        val frameSize = format.frameSize.coerceAtLeast(1)
        val frames = (pcm.size / frameSize).toLong()
        check(frames > 0) { "The speech audio file is empty." }
        // A short silent tail keeps some drivers from clipping the final syllable.
        val tailBytes = ((format.frameRate * 0.15f).toInt() * frameSize)
        val next = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
        next.open(format)
        Napier.d(
            "Desktop TTS playback started: bytes=${audio.size} frames=$frames frameRate=${format.frameRate} " +
                "durationSeconds=${frames / format.frameRate} buffer=${next.bufferSize}"
        )
        synchronized(lock) {
            line = next
            totalFrames = frames
            paused = false
            stopped = false
            next.start()
        }
        try {
            val step = (next.bufferSize / 4).coerceAtLeast(frameSize).let { it - it % frameSize }
            var offset = 0
            val data = pcm + ByteArray(tailBytes)
            while (offset < data.size) {
                currentCoroutineContext().ensureActive()
                if (synchronized(lock) { stopped }) break
                if (synchronized(lock) { paused }) {
                    delay(50)
                    continue
                }
                val written = next.write(data, offset, minOf(step, data.size - offset))
                if (written <= 0 && synchronized(lock) { stopped }) break
                offset += written
            }
            if (!synchronized(lock) { stopped }) {
                // Pausing stops the line, and drain() then waits until it is resumed.
                next.drain()
            }
            Napier.d(
                "Desktop TTS playback ended: playedFrames=${next.longFramePosition} frames=$frames " +
                    "stopped=${synchronized(lock) { stopped }}"
            )
        } finally {
            synchronized(lock) {
                if (line === next) line = null
                next.stop()
                next.flush()
                next.close()
            }
        }
    }

    override fun pause() {
        synchronized(lock) {
            if (!stopped) {
                paused = true
                line?.stop()
            }
        }
    }

    override fun resume() {
        synchronized(lock) {
            if (!stopped) {
                paused = false
                line?.start()
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            stopped = true
            paused = false
            line?.let {
                it.stop()
                it.flush()
                it.close()
            }
            line = null
        }
    }

    override fun progress(): Float? = synchronized(lock) {
        line?.let { current ->
            if (totalFrames <= 0) 0f
            else (current.longFramePosition.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
        }
    }

    override fun positionSeconds(): Float? = synchronized(lock) {
        line?.let { current ->
            if (current.format.frameRate <= 0f) null
            else current.longFramePosition.toFloat() / current.format.frameRate
        }
    }
}
