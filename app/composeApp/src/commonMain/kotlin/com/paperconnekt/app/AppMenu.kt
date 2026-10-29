package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier

enum class AppMenuGroup(val title: String) {
    FILE("File"), VIEW("View"), HELP("Help"), ABOUT("About"),
}

enum class AppMenuAction(
    val title: String,
    val group: AppMenuGroup,
    val requiresWorkspace: Boolean = true,
) {
    ADD_PAPER("Add paper...", AppMenuGroup.FILE),
    SETTINGS("Settings", AppMenuGroup.FILE),
    DASHBOARD("Dashboard", AppMenuGroup.VIEW),
    LIBRARY("Library", AppMenuGroup.VIEW),
    GRAPH("Graph", AppMenuGroup.VIEW),
    ASSISTANT("Assistant", AppMenuGroup.VIEW),
    THEME_SYSTEM("Use system theme", AppMenuGroup.VIEW),
    THEME_LIGHT("Light theme", AppMenuGroup.VIEW),
    THEME_DARK("Dark theme", AppMenuGroup.VIEW),
    GETTING_STARTED("Getting started", AppMenuGroup.HELP, false),
    SHORTCUTS("Keyboard shortcuts", AppMenuGroup.HELP, false),
    REPORT_ISSUE("Report an issue", AppMenuGroup.HELP, false),
    RELEASES("Release notes and updates", AppMenuGroup.HELP, false),
    ABOUT_APP("About PaperConnekt", AppMenuGroup.ABOUT, false),
    ABOUT_DEVELOPER("About the developer", AppMenuGroup.ABOUT, false),
    CONTACT("Contact the developer", AppMenuGroup.ABOUT, false),
    ACKNOWLEDGEMENTS("Acknowledgements", AppMenuGroup.ABOUT, false),
    QUIT("Quit", AppMenuGroup.FILE, false),
}

class AppMenuState(private val onExit: () -> Unit = {}) {
    var workspaceReady by mutableStateOf(false)
        internal set
    internal var onWorkspaceAction: ((AppMenuAction) -> Unit)? = null
    internal var information by mutableStateOf<AppMenuAction?>(null)
    internal var error by mutableStateOf<String?>(null)

    fun perform(action: AppMenuAction) {
        if (action.requiresWorkspace && !workspaceReady) return
        when (action.group) {
            AppMenuGroup.HELP, AppMenuGroup.ABOUT -> information = action
            else -> {
                if (action == AppMenuAction.QUIT && onWorkspaceAction == null) onExit()
                else onWorkspaceAction?.invoke(action)
            }
        }
    }

    internal fun exit() = onExit()

    fun reportError(message: String) {
        error = message
    }
}

@Composable
internal fun MobileAppMenu(state: AppMenuState) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("PaperConnekt", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Box {
            TextButton(onClick = { expanded = true }) { Text("Menu") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppMenuGroup.entries.forEach { group ->
                    if (group != AppMenuGroup.FILE) HorizontalDivider()
                    Text(
                        group.title,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppMenuAction.entries.filter {
                        it.group == group && it != AppMenuAction.QUIT && it != AppMenuAction.SHORTCUTS
                    }.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.title) },
                            enabled = !action.requiresWorkspace || state.workspaceReady,
                            onClick = {
                                expanded = false
                                state.perform(action)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AppMenuDialogs(state: AppMenuState, dialogHost: AppMenuDialogHost) {
    val uriHandler = LocalUriHandler.current
    val error = state.error
    if (error != null) {
        dialogHost.Dialog(
            title = "Action could not be completed",
            onDismiss = { state.error = null },
        ) {
            SelectionContainer { Text(error) }
        }
    } else {
        state.information?.let { action ->
            val content = informationContent(action)
            dialogHost.Dialog(
                title = action.title,
                onDismiss = { state.information = null },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(content.text)
                    content.url?.let { url ->
                        TextButton(onClick = {
                            try {
                                uriHandler.openUri(url)
                            } catch (error: Exception) {
                                Napier.e("Could not open $url", error)
                                state.reportError("Could not open the link. You can copy it into your browser:\n$url")
                            }
                        }) { Text(content.linkLabel) }
                        SelectionContainer {
                            Text(url, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

interface AppMenuDialogHost {
    @Composable
    fun Dialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit)
}

object MaterialAppMenuDialogHost : AppMenuDialogHost {
    @Composable
    override fun Dialog(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    content()
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        )
    }
}

private data class InformationContent(
    val text: String,
    val url: String? = null,
    val linkLabel: String = "Open in browser",
)

private fun informationContent(action: AppMenuAction): InformationContent = when (action) {
    AppMenuAction.GETTING_STARTED -> InformationContent(
        "Start in Dashboard to discover papers. Use File > Add paper on desktop, or Menu > Add paper " +
            "on Android, to save a reference manually.\n\n" +
            "Organise papers in Library and attach PDFs through each item's details. " +
            "Use Graph to connect papers and Assistant to discuss your research.\n\n" +
            "Your workspace stays on this device. Configure optional providers and appearance in Settings. " +
            "Provider-backed discovery and AI require an internet connection.",
    )
    AppMenuAction.SHORTCUTS -> InformationContent(
        "Use Command on macOS and Ctrl on Windows/Linux.\n\n" +
            "Command/Ctrl+N: Add paper\nCommand/Ctrl+,: Settings\n" +
            "Command/Ctrl+1: Dashboard\nCommand/Ctrl+2: Library\n" +
            "Command/Ctrl+3: Graph\nCommand/Ctrl+4: Assistant\n" +
            "Command/Ctrl+Q: Quit\nF11: Toggle fullscreen\n\n" +
            "Use Alt plus a menu's mnemonic on supported desktop systems.",
    )
    AppMenuAction.REPORT_ISSUE -> InformationContent(
        "Report a bug or suggest an improvement on GitHub. Include your platform, " +
            "what you expected, and steps to reproduce. Do not include provider keys or private research data.",
        "https://github.com/AkramBensalem/PaperConnekt/issues/new",
        "Report an issue",
    )
    AppMenuAction.RELEASES -> InformationContent(
        "Visit GitHub releases for published versions and release notes. " +
            "PaperConnekt does not check for or install updates automatically.",
        "https://github.com/AkramBensalem/PaperConnekt/releases",
        "View releases",
    )
    AppMenuAction.ABOUT_APP -> InformationContent(
        "PaperConnekt is a local-first Kotlin Multiplatform research workspace for reading, " +
            "organising, connecting, and discussing scientific literature.\n\n" +
            "No mandatory account or remote backend is required. Optional providers extend the local workspace.",
        "https://github.com/AkramBensalem/PaperConnekt",
        "View project",
    )
    AppMenuAction.ABOUT_DEVELOPER -> InformationContent(
        "Created by Akram Bensalem. PaperConnekt grew out of a researcher's need to keep papers, " +
            "notes, and ideas together.",
        "https://github.com/AkramBensalem",
        "Developer profile",
    )
    AppMenuAction.CONTACT -> InformationContent(
        "For questions, feedback, or collaboration, visit the developer's public GitHub profile. " +
            "For reproducible bugs, use Help > Report an issue.",
        "https://github.com/AkramBensalem",
        "Contact through GitHub",
    )
    AppMenuAction.ACKNOWLEDGEMENTS -> InformationContent(
        "Built with Kotlin Multiplatform and Compose Multiplatform, alongside the open-source " +
            "libraries listed in the repository. Optional integrations include OpenAlex, Zotero, " +
            "and AI providers.\n\nSee the repository for dependency declarations and licensing information.",
        "https://github.com/AkramBensalem/PaperConnekt",
        "Source and licensing",
    )
    else -> error("Not an information action: $action")
}
