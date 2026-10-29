package com.paperconnekt.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppMenuStateTest {
    @Test
    fun workspaceActionsWaitForInitializationAndOnboarding() {
        val state = AppMenuState()
        val actions = mutableListOf<AppMenuAction>()
        state.onWorkspaceAction = { actions += it }

        AppMenuAction.entries.filter { it.requiresWorkspace }.forEach(state::perform)
        assertTrue(actions.isEmpty())

        state.workspaceReady = true
        AppMenuAction.entries.filter { it.requiresWorkspace }.forEach(state::perform)
        assertEquals(AppMenuAction.entries.filter { it.requiresWorkspace }, actions)
    }

    @Test
    fun helpAndAboutAreAvailableWithoutAWorkspace() {
        val state = AppMenuState()
        state.onWorkspaceAction = { error("Information must not navigate away from the workspace") }

        AppMenuAction.entries.filter {
            it.group == AppMenuGroup.HELP || it.group == AppMenuGroup.ABOUT
        }.forEach { action ->
            state.perform(action)
            assertEquals(action, state.information)
        }
    }

    @Test
    fun quitCanExitWhenTheWorkspaceHasNotLoaded() {
        var exited = false
        val state = AppMenuState(onExit = { exited = true })
        state.perform(AppMenuAction.QUIT)
        assertTrue(exited)
    }

    @Test
    fun quitWaitsForThePdfNavigationGuard() {
        var exited = false
        var confirmation: (() -> Unit)? = null
        val state = AppMenuState(onExit = { exited = true })
        val guard = PdfNavigationGuard()
        guard.beforeNavigate = { confirmation = it }
        state.onWorkspaceAction = { action ->
            assertEquals(AppMenuAction.QUIT, action)
            guard.navigate { state.exit() }
        }

        state.perform(AppMenuAction.QUIT)
        assertFalse(exited)
        confirmation?.invoke()
        assertTrue(exited)
    }

    @Test
    fun informationDoesNotInvokeThePdfNavigationGuard() {
        var guarded = false
        val state = AppMenuState()
        state.onWorkspaceAction = { guarded = true }
        state.perform(AppMenuAction.ABOUT_APP)
        assertFalse(guarded)
        assertEquals(AppMenuAction.ABOUT_APP, state.information)
        assertNull(state.error)
    }

    @Test
    fun platformFailuresHaveVisibleFeedback() {
        val state = AppMenuState()
        state.reportError("No file manager is available.")
        assertEquals("No file manager is available.", state.error)
    }
}
