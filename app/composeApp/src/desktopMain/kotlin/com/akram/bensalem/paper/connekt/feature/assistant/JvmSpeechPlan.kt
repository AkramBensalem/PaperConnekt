package com.akram.bensalem.paper.connekt.feature.assistant

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

internal fun saveJvmSpeechPlanJson(fileName: String, json: String): Boolean {
    val dialog = FileDialog(null as Frame?, "Save speech plan", FileDialog.SAVE).apply {
        this.file = fileName
        isVisible = true
    }
    val directory = dialog.directory ?: return false
    val selected = dialog.file ?: return false
    File(directory, selected).writeText(json)
    return true
}
