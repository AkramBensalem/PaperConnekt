package com.paperconnekt.domain.navigation

import kotlinx.serialization.Serializable

@Serializable
enum class AppDestination {
    ONBOARDING,
    DASHBOARD,
    ASSISTANT,
    GRAPH,
    GRAPH_CANVAS,
    LIBRARY,
    SETTINGS,
}

