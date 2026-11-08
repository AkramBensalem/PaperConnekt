package com.paperconnekt.idea

import com.akram.bensalem.paper.connekt.core.settings.createApiKeysStore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.FormBuilder
import com.paperconnekt.secrets.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPasswordField

private data class NativeProviderValues(
    val openAi: String,
    val mistral: String,
    val zotero: String,
    val zoteroUserId: String,
    val openAlex: String,
)

internal suspend fun editNativeProviderSettings(project: Project, features: NativeFeatures) {
    val openAi = features.secrets.read(SecretKey.OPENAI).orEmpty()
    val mistral = features.secrets.read(SecretKey.MISTRAL).orEmpty()
    val zotero = features.secrets.read(SecretKey.ZOTERO).orEmpty()
    val zoteroUserId = features.zoteroUserId()
    val openAlex = features.secrets.read(SecretKey.OPENALEX).orEmpty()
    val updated = withContext(Dispatchers.Swing) {
        val dialog = object : DialogWrapper(project) {
            val openAiField = JPasswordField(openAi, 32)
            val mistralField = JPasswordField(mistral, 32)
            val zoteroField = JPasswordField(zotero, 32)
            val zoteroUserIdField = javax.swing.JTextField(zoteroUserId, 32)
            val openAlexField = JPasswordField(openAlex, 32)
            init {
                title = "PaperConnekt provider settings"
                init()
            }
            override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
                .addComponent(JLabel("Optional. Reading and annotations work without provider keys."))
                .addLabeledComponent("OpenAI API key", openAiField)
                .addLabeledComponent("Mistral API key", mistralField)
                .addLabeledComponent("Zotero API key", zoteroField)
                .addLabeledComponent("Zotero numeric user ID", zoteroUserIdField)
                .addLabeledComponent("OpenAlex API key", openAlexField)
                .addComponent(JLabel("Find the numeric user ID in your Zotero account settings."))
                .addComponent(JLabel("AI chat sends selected context to the chosen provider."))
                .addComponent(JLabel("Mistral OCR sends the PDF when you request Markdown conversion."))
                .addComponent(JLabel("Keys stay in IntelliJ Password Safe. Clear a field to remove its key."))
                .panel
        }
        try {
            if (dialog.showAndGet()) {
                NativeProviderValues(
                    openAi = String(dialog.openAiField.password).trim(),
                    mistral = String(dialog.mistralField.password).trim(),
                    zotero = String(dialog.zoteroField.password).trim(),
                    zoteroUserId = dialog.zoteroUserIdField.text.trim(),
                    openAlex = String(dialog.openAlexField.password).trim(),
                )
            } else null
        } finally {
            dialog.openAiField.text = ""
            dialog.mistralField.text = ""
            dialog.zoteroField.text = ""
            dialog.zoteroUserIdField.text = ""
            dialog.openAlexField.text = ""
        }
    } ?: return
    for ((key, value) in listOf(
        SecretKey.OPENAI to updated.openAi,
        SecretKey.MISTRAL to updated.mistral,
        SecretKey.ZOTERO to updated.zotero,
        SecretKey.OPENALEX to updated.openAlex,
    )) {
        if (value.isBlank()) features.secrets.delete(key) else features.secrets.write(key, value)
    }
    features.saveZoteroUserId(updated.zoteroUserId)
    val keys = createApiKeysStore()
    keys.save(keys.apiKeys.value.copy(openAi = updated.openAi, mistral = updated.mistral, zotero = updated.zotero))
    features.agent.refreshProviderKey()
}
