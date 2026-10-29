package com.paperconnekt.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.v2.Window
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowPositionProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.v2.rememberWindowState
import com.paperconnekt.app.AppMenuAction
import com.paperconnekt.app.AppMenuGroup
import com.paperconnekt.app.AppMenuState
import com.paperconnekt.app.PaperConnektApp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.paperconnekt_icon
import com.paperconnekt.storage.DesktopWorkspaceStorage
import com.paperconnekt.storage.desktopAppDataDirectory
import java.awt.Desktop
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val menuState = remember { AppMenuState(onExit = ::exitApplication) }
    val windowState = rememberWindowState(
        initialBoundsProvider = WindowBoundsProvider(
            positionProvider = WindowPositionProvider.CenteredInScreenBounds,
            sizeProvider = WindowSizeProvider.Fixed(DpSize(1100.dp, 760.dp)),
        ),
    )
    var previousPlacement by remember { mutableStateOf(WindowPlacement.Floating) }
    val scope = rememberCoroutineScope()
    val isMac = remember { System.getProperty("os.name").startsWith("Mac", ignoreCase = true) }
    Window(
        onCloseRequest = { menuState.perform(AppMenuAction.QUIT) },
        title = "PaperConnekt",
        icon = painterResource(Res.drawable.paperconnekt_icon),
        state = windowState,
        minSize = DpSize(640.dp, 480.dp),
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
                            enabled = windowState.isInitialized,
                            checked = windowState.isInitialized &&
                                windowState.placement == WindowPlacement.Fullscreen,
                            shortcut = KeyShortcut(Key.F11),
                            onCheckedChange = { fullscreen ->
                                if (fullscreen) {
                                    previousPlacement = windowState.placement
                                    windowState.requestPlacement(WindowPlacement.Fullscreen)
                                } else {
                                    windowState.requestPlacement(previousPlacement)
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
            menuDialogHost = DesktopMenuDialogHost,
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
