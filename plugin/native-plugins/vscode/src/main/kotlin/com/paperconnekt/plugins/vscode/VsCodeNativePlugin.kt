package com.paperconnekt.plugins.vscode

import com.paperconnekt.plugins.common.NativePluginSession

class VsCodeNativePlugin : NativePluginSession() {
    override val id: String = "paperconnekt.vscode"
    override val displayName: String = "PaperConnekt for VS Code"
}
