package com.paperconnekt.webserver

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticFiles
import java.io.File

fun main() {
    val host = System.getenv("PAPERCONNEKT_HOST") ?: "0.0.0.0"
    val port = System.getenv("PAPERCONNEKT_PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, host = host, port = port, module = Application::paperConnektModule)
        .start(wait = true)
}

fun Application.paperConnektModule() {
    install(Compression)
    val webDirectory = File(
        System.getProperty("paperconnekt.web.dir")
            ?: System.getenv("PAPERCONNEKT_WEB_DIR")
            ?: "app/web/build/dist/wasmJs/productionExecutable",
    )

    routing {
        get("/health") {
            call.respondText("ok", contentType = ContentType.Text.Plain)
        }

        if (webDirectory.isDirectory) {
            staticFiles("/", webDirectory) {
                default("index.html")
                contentType { file ->
                    when {
                        file.extension.equals("wasm", ignoreCase = true) ->
                            ContentType("application", "wasm")
                        file.extension.equals("mjs", ignoreCase = true) ->
                            ContentType.Application.JavaScript
                        else -> null
                    }
                }
            }
        } else {
            get("/") {
                call.respondText(
                    "The web distribution was not found. Build :app:web:wasmJsBrowserDistribution first.",
                    status = HttpStatusCode.NotFound,
                )
            }
        }
    }
}
