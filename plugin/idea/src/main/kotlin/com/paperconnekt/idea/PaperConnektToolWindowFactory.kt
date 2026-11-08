package com.paperconnekt.idea

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.util.ui.JBUI
import org.jetbrains.jewel.bridge.addComposeTab
import androidx.compose.runtime.CompositionLocalProvider

class PaperConnektToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.component.minimumSize = JBUI.size(560, 420)
        val shortcuts = NativeShortcuts().also { it.install(toolWindow.component, toolWindow.disposable) }
        toolWindow.addComposeTab("Research", focusOnClickInside = true) {
            CompositionLocalProvider(LocalNativeShortcuts provides shortcuts) {
                PaperConnektWorkspace(project)
            }
        }
    }
}
