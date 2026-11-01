package com.paperconnekt.web

import com.paperconnekt.domain.navigation.AppDestination

internal object BrowserHistory {
    private var firstChange = true

    fun initialDestination(): AppDestination? = destinationFromPath(browserCurrentPath())

    fun listen(onDestinationRequested: (String) -> Unit) =
        listenToBrowserHistory(onDestinationRequested)

    fun record(destination: AppDestination) {
        val path = pathFor(destination)
        if (firstChange) {
            browserReplacePath(path)
            firstChange = false
        } else if (browserCurrentPath() != path) {
            browserPushPath(path)
        }
    }

    private fun destinationFromPath(path: String): AppDestination? = when {
        path == "/" || path == "/dashboard" -> AppDestination.DASHBOARD
        path == "/assistant" -> AppDestination.ASSISTANT
        path == "/assistant/chat" -> AppDestination.ASSISTANT_CHAT
        path == "/graph" -> AppDestination.GRAPH
        path == "/library" -> AppDestination.LIBRARY
        path == "/settings" -> AppDestination.SETTINGS
        else -> null
    }

    private fun pathFor(destination: AppDestination): String = when (destination) {
        AppDestination.ONBOARDING -> "/"
        AppDestination.DASHBOARD -> "/dashboard"
        AppDestination.ASSISTANT -> "/assistant"
        AppDestination.ASSISTANT_CHAT -> "/assistant/chat"
        AppDestination.GRAPH, AppDestination.GRAPH_CANVAS -> "/graph"
        AppDestination.LIBRARY -> "/library"
        AppDestination.SETTINGS -> "/settings"
    }
}

private fun listenToBrowserHistory(onDestinationRequested: (String) -> Unit): Unit = js(
    """
    {
        window.addEventListener("popstate", function() {
            const path = window.location.pathname;
            let destination = null;
            if (path === "/" || path === "/dashboard") destination = "DASHBOARD";
            else if (path === "/assistant") destination = "ASSISTANT";
            else if (path === "/assistant/chat") destination = "ASSISTANT_CHAT";
            else if (path === "/graph") destination = "GRAPH";
            else if (path === "/library") destination = "LIBRARY";
            else if (path === "/settings") destination = "SETTINGS";
            if (destination != null) onDestinationRequested(destination);
        });
    }
    """,
)

private fun browserCurrentPath(): String = js("window.location.pathname")

private fun browserReplacePath(path: String): Unit = js("window.history.replaceState({}, '', path)")

private fun browserPushPath(path: String): Unit = js("window.history.pushState({}, '', path)")
