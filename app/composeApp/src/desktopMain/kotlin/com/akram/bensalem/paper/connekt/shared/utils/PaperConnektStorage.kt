package com.akram.bensalem.paper.connekt.shared.utils

import java.nio.file.Path
import com.paperconnekt.storage.desktopAppDataDirectory

/** Hosts may isolate their stores without changing the native apps' default data directory. */
fun paperConnektDataDirectory(): Path = desktopAppDataDirectory()
