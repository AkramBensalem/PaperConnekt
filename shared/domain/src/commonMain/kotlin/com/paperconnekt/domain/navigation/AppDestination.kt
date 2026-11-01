package com.paperconnekt.domain.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class AppDestination {
    ONBOARDING,
    DASHBOARD,
    ASSISTANT,
    ASSISTANT_CHAT,
    GRAPH,
    GRAPH_CANVAS,
    LIBRARY,
    SETTINGS,
}
