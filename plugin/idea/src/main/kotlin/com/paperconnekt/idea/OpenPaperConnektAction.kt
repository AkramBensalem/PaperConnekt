package com.paperconnekt.idea

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

class OpenPaperConnektAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        event.project?.let { project ->
            ToolWindowManager.getInstance(project).getToolWindow("PaperConnekt")?.show()
        }
    }
}
