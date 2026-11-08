package com.paperconnekt.idea

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.project.DumbAwareAction
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

private val menuMask: Int get() = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

/** Workspace keyboard shortcuts, matching the app's reader/search bindings (Ctrl is Cmd on macOS). */
internal enum class NativeShortcut(val strokes: () -> List<KeyStroke>) {
    Find({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuMask)) }),
    FindNext({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)) }),
    FindPrevious({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK)) }),
    GoToPage({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_G, menuMask)) }),
    ZoomIn({
        listOf(
            KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuMask),
            KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, menuMask or InputEvent.SHIFT_DOWN_MASK),
            KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, menuMask),
            KeyStroke.getKeyStroke(KeyEvent.VK_ADD, menuMask),
        )
    }),
    ZoomOut({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, menuMask), KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, menuMask)) }),
    ZoomReset({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_0, menuMask), KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, menuMask)) }),
    Copy({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask)) }),
    SelectAll({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask)) }),
    PreviousPage({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0)) }),
    NextPage({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)) }),
    FirstPage({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, menuMask)) }),
    LastPage({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_END, menuMask)) }),
    Escape({ listOf(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)) }),
}

/**
 * Shortcuts are registered as IntelliJ actions local to the tool window, so they take priority over
 * global keymap bindings (e.g. Ctrl+F "Find") while focus is inside the workspace. A shortcut with no
 * enabled handler stays disabled and the key event continues to text fields and the IDE as usual.
 */
internal class NativeShortcuts {
    private class Entry(val shortcut: NativeShortcut, val enabled: () -> Boolean, val action: () -> Unit)

    private val entries = mutableListOf<Entry>()

    private fun active(shortcut: NativeShortcut): Entry? = entries.lastOrNull { it.shortcut == shortcut && it.enabled() }

    fun register(shortcut: NativeShortcut, enabled: () -> Boolean, action: () -> Unit): () -> Unit {
        val entry = Entry(shortcut, enabled, action)
        entries += entry
        return { entries -= entry }
    }

    fun install(component: JComponent, parent: Disposable) {
        NativeShortcut.entries.forEach { shortcut ->
            object : DumbAwareAction() {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(event: AnActionEvent) {
                    event.presentation.isEnabled = active(shortcut) != null
                }
                override fun actionPerformed(event: AnActionEvent) {
                    active(shortcut)?.action?.invoke()
                }
            }.registerCustomShortcutSet(
                CustomShortcutSet(*shortcut.strokes().map { KeyboardShortcut(it, null) }.toTypedArray()),
                component, parent,
            )
        }
    }
}

internal val LocalNativeShortcuts = staticCompositionLocalOf<NativeShortcuts?> { null }

/** Binds [shortcut] to [action] while this composable is in the composition and [enabled] is true. */
@Composable
internal fun NativeShortcutHandler(shortcut: NativeShortcut, enabled: Boolean = true, action: () -> Unit) {
    val shortcuts = LocalNativeShortcuts.current ?: return
    val currentEnabled by rememberUpdatedState(enabled)
    val currentAction by rememberUpdatedState(action)
    DisposableEffect(shortcuts, shortcut) {
        val unregister = shortcuts.register(shortcut, { currentEnabled }, { currentAction() })
        onDispose { unregister() }
    }
}
