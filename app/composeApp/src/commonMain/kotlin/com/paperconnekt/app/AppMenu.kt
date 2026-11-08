package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_close
import com.paperconnekt.app.resources.menu
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.paperconnekt.app.resources.about_developer
import com.paperconnekt.app.resources.about_paperconnekt
import com.paperconnekt.app.resources.acknowledgements
import com.paperconnekt.app.resources.add_paper_menu
import com.paperconnekt.app.resources.app_name
import com.paperconnekt.app.resources.appearance
import com.paperconnekt.app.resources.close
import com.paperconnekt.app.resources.contact_developer
import com.paperconnekt.app.resources.dark_theme
import com.paperconnekt.app.resources.error_action_failed
import com.paperconnekt.app.resources.getting_started
import com.paperconnekt.app.resources.help_about
import com.paperconnekt.app.resources.keyboard_shortcuts
import com.paperconnekt.app.resources.light_theme
import com.paperconnekt.app.resources.local_first_workspace
import com.paperconnekt.app.resources.manage
import com.paperconnekt.app.resources.nav_assistant
import com.paperconnekt.app.resources.nav_dashboard
import com.paperconnekt.app.resources.nav_graph
import com.paperconnekt.app.resources.nav_library
import com.paperconnekt.app.resources.nav_settings
import com.paperconnekt.app.resources.quick_actions
import com.paperconnekt.app.resources.quit
import com.paperconnekt.app.resources.release_notes
import com.paperconnekt.app.resources.report_issue
import com.paperconnekt.app.resources.use_system_theme

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

@Composable
private fun AppMenuAction.localizedTitle(): String = when (this) {
    AppMenuAction.ADD_PAPER -> stringResource(Res.string.add_paper_menu)
    AppMenuAction.SETTINGS -> stringResource(Res.string.nav_settings)
    AppMenuAction.DASHBOARD -> stringResource(Res.string.nav_dashboard)
    AppMenuAction.LIBRARY -> stringResource(Res.string.nav_library)
    AppMenuAction.GRAPH -> stringResource(Res.string.nav_graph)
    AppMenuAction.ASSISTANT -> stringResource(Res.string.nav_assistant)
    AppMenuAction.THEME_SYSTEM -> stringResource(Res.string.use_system_theme)
    AppMenuAction.THEME_LIGHT -> stringResource(Res.string.light_theme)
    AppMenuAction.THEME_DARK -> stringResource(Res.string.dark_theme)
    AppMenuAction.GETTING_STARTED -> stringResource(Res.string.getting_started)
    AppMenuAction.SHORTCUTS -> stringResource(Res.string.keyboard_shortcuts)
    AppMenuAction.REPORT_ISSUE -> stringResource(Res.string.report_issue)
    AppMenuAction.RELEASES -> stringResource(Res.string.release_notes)
    AppMenuAction.ABOUT_APP -> stringResource(Res.string.about_paperconnekt)
    AppMenuAction.ABOUT_DEVELOPER -> stringResource(Res.string.about_developer)
    AppMenuAction.CONTACT -> stringResource(Res.string.contact_developer)
    AppMenuAction.ACKNOWLEDGEMENTS -> stringResource(Res.string.acknowledgements)
    AppMenuAction.QUIT -> stringResource(Res.string.quit)
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
internal fun MobileAppMenu(
    drawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onDrawerOpenChange(!drawerOpen) },
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Icon(
                painter = painterResource(if (drawerOpen) Res.drawable.filled_close else Res.drawable.menu),
                contentDescription = if (drawerOpen) {
                    stringResource(Res.string.close)
                } else {
                    stringResource(Res.string.app_name)
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.local_first_workspace),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MobileAppDrawer(
    state: AppMenuState,
    drawerWidth: Dp,
    onDismiss: () -> Unit,
) {
    var manageExpanded by remember { mutableStateOf(true) }
    var appearanceExpanded by remember { mutableStateOf(true) }
    var supportExpanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = drawerWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PaperConnekt", style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(Res.string.quick_actions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            DrawerSection(
                title = stringResource(Res.string.appearance),
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded },
            ) {
                listOf(
                    AppMenuAction.THEME_SYSTEM,
                    AppMenuAction.THEME_LIGHT,
                    AppMenuAction.THEME_DARK,
                ).forEach { action ->
                    DrawerAction(
                        action = action,
                        enabled = !action.requiresWorkspace || state.workspaceReady,
                        onClick = {
                            onDismiss()
                            state.perform(action)
                        },
                    )
                }
            }

            DrawerSection(
                title = stringResource(Res.string.help_about),
                expanded = supportExpanded,
                onToggle = { supportExpanded = !supportExpanded },
            ) {
                AppMenuAction.entries.filter {
                    (it.group == AppMenuGroup.HELP || it.group == AppMenuGroup.ABOUT) &&
                        it != AppMenuAction.SHORTCUTS &&
                        it != AppMenuAction.ABOUT_DEVELOPER
                }.forEach { action ->
                    DrawerAction(
                        action = action,
                        title = when (action) {
                            AppMenuAction.ABOUT_APP -> "About us"
                            AppMenuAction.CONTACT -> "Contact us"
                            else -> action.localizedTitle()
                        },
                        enabled = !action.requiresWorkspace || state.workspaceReady,
                        onClick = { state.perform(action) },
                    )
                }
            }

            DrawerSection(
                title = stringResource(Res.string.manage),
                expanded = manageExpanded,
                onToggle = { manageExpanded = !manageExpanded },
            ) {
                listOf(AppMenuAction.ADD_PAPER, AppMenuAction.SETTINGS).forEach { action ->
                    DrawerAction(
                        action = action,
                        enabled = !action.requiresWorkspace || state.workspaceReady,
                        onClick = {
                            onDismiss()
                            state.perform(action)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (expanded) "−" else "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun DrawerAction(
    action: AppMenuAction,
    title: String? = null,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                title ?: action.localizedTitle(),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun AppMenuDialogs(state: AppMenuState, dialogHost: AppMenuDialogHost) {
    val uriHandler = LocalUriHandler.current
    val error = state.error
    if (error != null) {
        dialogHost.Dialog(
            title = stringResource(Res.string.error_action_failed),
            onDismiss = { state.error = null },
        ) {
            SelectionContainer { Text(error) }
        }
    } else {
        state.information?.let { action ->
            val content = informationContent(action)
            dialogHost.Dialog(
                title = action.localizedTitle(),
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
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
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
        "PaperConnekt is a local-first research workspace created by Akram Bensalem. " +
            "It helps researchers organise papers, connect ideas, and discuss scientific literature.\n\n" +
            "The project is built with Kotlin Multiplatform and Compose Multiplatform. " +
            "No mandatory account or remote backend is required.",
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
        "For questions, feedback, or collaboration, contact Akram Bensalem through the public GitHub profile. " +
            "For reproducible bugs, use Report an issue.",
        "https://github.com/AkramBensalem",
        "Contact us through GitHub",
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
