package com.paperconnekt.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.paperconnekt.app.AppMenuAction
import com.paperconnekt.app.AppMenuGroup
import com.paperconnekt.app.AppMenuState
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.storage.DesktopWorkspaceStorage
import com.paperconnekt.storage.desktopAppDataDirectory
import java.awt.Desktop
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() = application {
    val menuState = remember { AppMenuState(onExit = ::exitApplication) }
    val windowState = rememberWindowState()
    val scope = rememberCoroutineScope()
    val isMac = remember { System.getProperty("os.name").startsWith("Mac", ignoreCase = true) }
    Window(
        onCloseRequest = { menuState.perform(AppMenuAction.QUIT) },
        title = "PaperConnekt",
        icon = painterResource("icons/paperconnekt.png"),
        state = windowState,
    ) {
        MenuBar {
            AppMenuGroup.entries.forEach { group ->
                Menu(group.title, mnemonic = group.title.first()) {
                    AppMenuAction.entries.filter {
                        it.group == group && it != AppMenuAction.QUIT
                    }.forEach { action ->
                        Item(
                            text = action.title,
                            enabled = !action.requiresWorkspace || menuState.workspaceReady,
                            shortcut = menuShortcut(action, isMac),
                            onClick = { menuState.perform(action) },
                        )
                    }
                    if (group == AppMenuGroup.FILE) {
                        Separator()
                        Item(
                            "Open workspace folder",
                            enabled = menuState.workspaceReady,
                            onClick = {
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val folder = desktopAppDataDirectory().toFile()
                                            check(folder.isDirectory) { "Workspace folder does not exist: $folder" }
                                            check(Desktop.isDesktopSupported()) {
                                                "This desktop does not support opening folders."
                                            }
                                            val desktop = Desktop.getDesktop()
                                            check(desktop.isSupported(Desktop.Action.OPEN)) {
                                                "No system file manager is available."
                                            }
                                            desktop.open(folder)
                                        }
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (error: Exception) {
                                        Logger.getLogger("PaperConnekt").log(
                                            Level.WARNING, "Could not open workspace folder", error,
                                        )
                                        menuState.reportError(
                                            "Could not open the workspace folder: ${error.message}",
                                        )
                                    }
                                }
                            },
                        )
                        Separator()
                        Item(
                            "Quit",
                            shortcut = menuShortcut(AppMenuAction.QUIT, isMac),
                            onClick = { menuState.perform(AppMenuAction.QUIT) },
                        )
                    }
                    if (group == AppMenuGroup.VIEW) {
                        Separator()
                        CheckboxItem(
                            "Fullscreen",
                            checked = windowState.placement == WindowPlacement.Fullscreen,
                            shortcut = KeyShortcut(Key.F11),
                            onCheckedChange = { fullscreen ->
                                windowState.placement = if (fullscreen) {
                                    WindowPlacement.Fullscreen
                                } else {
                                    WindowPlacement.Floating
                                }
                            },
                        )
                    }
                }
            }
        }
        PaperConnektApp(
            secretStore = remember { DesktopSecretStore() },
            workspaceStorage = remember { DesktopWorkspaceStorage() },
            pdfAttachments = remember { DesktopPdfAttachmentUi() },
            menuState = menuState,
        )
    }
}

private fun menuShortcut(action: AppMenuAction, isMac: Boolean): KeyShortcut? {
    val key = when (action) {
        AppMenuAction.ADD_PAPER -> Key.N
        AppMenuAction.SETTINGS -> Key.Comma
        AppMenuAction.DASHBOARD -> Key.One
        AppMenuAction.LIBRARY -> Key.Two
        AppMenuAction.GRAPH -> Key.Three
        AppMenuAction.ASSISTANT -> Key.Four
        AppMenuAction.QUIT -> Key.Q
        else -> return null
    }
    return KeyShortcut(key, meta = isMac, ctrl = !isMac)
}
