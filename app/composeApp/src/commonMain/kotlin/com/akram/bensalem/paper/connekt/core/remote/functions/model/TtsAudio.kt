package com.akram.bensalem.paper.connekt.core.remote.functions.model

import kotlinx.serialization.Serializable

@Serializable
data class TtsAudio(
    val audioUrl: String,
    val path: String,
    val cached: Boolean = false,
    val timepoints: List<TtsTimepoint> = emptyList(),
)

@Serializable
data class TtsTimepoint(
    val markName: String,
    val timeSeconds: Float,
)
