package com.akram.bensalem.paper.connekt.feature.assistant.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.akram.bensalem.paper.connekt.feature.assistant.component.TouchpadGestureDetector.Result
import io.github.aakira.napier.Napier
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

@Composable
internal actual fun NativeReaderTouchpad(
    enabled: () -> Boolean,
    onZoom: (Float) -> Unit,
    onHorizontalPan: (Float) -> Unit,
) {
    val isEnabled by rememberUpdatedState(enabled)
    val zoom by rememberUpdatedState(onZoom)
    val pan by rememberUpdatedState(onHorizontalPan)
    val events = remember { Channel<Result>(Channel.UNLIMITED) }
    LaunchedEffect(Unit) {
        events.receiveAsFlow().collect { result ->
            if (!isEnabled()) return@collect
            when (result) {
                is Result.Zoom -> zoom(result.factor)
                // Vertical swipes already arrive as scroll events; only horizontal ones are missing on X11.
                is Result.Pan -> if (result.dx != 0f) pan(result.dx * PAN_PIXELS_PER_UNIT)
            }
        }
    }
    DisposableEffect(Unit) {
        val bridge = LinuxTouchpadBridge { events.trySend(it) }
        bridge.start()
        onDispose { bridge.close(); events.close() }
    }
}

private const val PAN_PIXELS_PER_UNIT = 1.2f

private class LinuxTouchpadBridge(private val onGesture: (Result) -> Unit) {
    @Volatile private var worker: Thread? = null
    @Volatile private var input: InputStream? = null

    fun start() {
        if (!System.getProperty("os.name").contains("Linux", ignoreCase = true)) return
        val device = findTouchpad() ?: return Napier.w(tag = TAG, message = "[NativeTouchpad] No readable touchpad device")
        worker = Thread({ run(device) }, "paperconnekt-reader-touchpad").apply { isDaemon = true; start() }
    }

    fun close() {
        worker?.interrupt()
        runCatching { input?.close() }
        worker = null; input = null
    }

    private fun run(device: File) {
        val xs = FloatArray(MAX_SLOTS) { Float.NaN }
        val ys = FloatArray(MAX_SLOTS) { Float.NaN }
        val ids = IntArray(MAX_SLOTS) { -1 }
        var slot = 0
        val detector = TouchpadGestureDetector()
        val buffer = ByteArray(EVENT_SIZE)
        try {
            FileInputStream(device).use { stream ->
                input = stream
                while (!Thread.currentThread().isInterrupted) {
                    if (!readFully(stream, buffer)) break
                    val type = u16(buffer, 16); val code = u16(buffer, 18); val value = s32(buffer, 20)
                    if (type == EV_ABS) when (code) {
                        ABS_MT_SLOT -> slot = value.coerceIn(0, MAX_SLOTS - 1)
                        ABS_MT_TRACKING_ID -> { ids[slot] = value; if (value < 0) { xs[slot] = Float.NaN; ys[slot] = Float.NaN } }
                        ABS_MT_POSITION_X -> xs[slot] = value.toFloat()
                        ABS_MT_POSITION_Y -> ys[slot] = value.toFloat()
                    } else if (type == EV_SYN && code == SYN_REPORT) {
                        val active = ids.indices.filter { ids[it] >= 0 && !xs[it].isNaN() && !ys[it].isNaN() }
                        if (active.size == 2) {
                            val (a, b) = active
                            val result = detector.update(xs[a], ys[a], xs[b], ys[b])
                            if (result is Result.Pan) TouchpadDirection.observeFingers(result.dy)
                            result?.let(onGesture)
                        } else detector.reset()
                    }
                }
            }
        } catch (error: IOException) {
            if (!Thread.currentThread().isInterrupted) Napier.w(tag = TAG, message = "[NativeTouchpad] ${error.message}")
        } catch (_: SecurityException) {
            Napier.w(tag = TAG, message = "[NativeTouchpad] Permission denied for $device; add the user to the input group")
        }
    }

    private fun findTouchpad(): File? = runCatching { File("/proc/bus/input/devices").readText() }.getOrNull()
        ?.split("\n\n")?.asSequence()
        ?.filter { block ->
            val text = block.lowercase()
            listOf("touchpad", "trackpad", "clickpad", "synaptics", "elan").any(text::contains) && "abs=" in text
        }
        ?.mapNotNull { Regex("""Handlers=.*?\bevent(\d+)\b""").find(it)?.groupValues?.get(1) }
        ?.map { File("/dev/input/event$it") }
        ?.firstOrNull { it.canRead() }

    private fun readFully(stream: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val count = stream.read(buffer, offset, buffer.size - offset)
            if (count < 0) return false
            offset += count
        }
        return true
    }

    private fun u16(b: ByteArray, o: Int) = (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8)
    private fun s32(b: ByteArray, o: Int) = (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8) or
        ((b[o + 2].toInt() and 0xff) shl 16) or (b[o + 3].toInt() shl 24)

    private companion object {
        const val TAG = "PdfReader"
        const val EVENT_SIZE = 24
        const val MAX_SLOTS = 16
        const val EV_SYN = 0
        const val EV_ABS = 3
        const val SYN_REPORT = 0
        const val ABS_MT_SLOT = 47
        const val ABS_MT_POSITION_X = 53
        const val ABS_MT_POSITION_Y = 54
        const val ABS_MT_TRACKING_ID = 57
    }
}
