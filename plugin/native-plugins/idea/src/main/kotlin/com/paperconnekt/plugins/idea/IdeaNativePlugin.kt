package com.paperconnekt.plugins.idea

import com.paperconnekt.plugins.common.NativePluginSession

class IdeaNativePlugin : NativePluginSession() {
    override val id: String = "paperconnekt.idea"
    override val displayName: String = "PaperConnekt for IntelliJ IDEA"
}
