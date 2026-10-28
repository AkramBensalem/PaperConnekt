package com.paperconnekt.plugin.api

import com.paperconnekt.domain.model.Paper

interface NativePlugin {
    val id: String
    val displayName: String

    suspend fun search(query: String): List<Paper>
}

