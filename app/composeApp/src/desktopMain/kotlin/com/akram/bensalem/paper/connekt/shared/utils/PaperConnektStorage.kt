package com.akram.bensalem.paper.connekt.shared.utils

import java.nio.file.Path
import java.nio.file.Paths

/** Hosts may isolate their stores without changing the native apps' default data directory. */
fun paperConnektDataDirectory(): Path =
    System.getProperty("paperconnekt.data.dir")?.let {
        require(it.isNotBlank()) { "paperconnekt.data.dir must not be blank." }
        Paths.get(it)
    } ?: Paths.get(System.getProperty("user.home"), ".paperconnekt")
