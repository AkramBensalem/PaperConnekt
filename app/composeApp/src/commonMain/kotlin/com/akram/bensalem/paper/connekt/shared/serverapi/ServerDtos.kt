package com.akram.bensalem.paper.connekt.shared.serverapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON configuration used on both sides of the wire. */
val ServerJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable
data class OptionalText(val value: String? = null)

@Serializable
data class KeyValue(val key: String, val value: String? = null)

@Serializable
data class StringSet(val values: Set<String> = emptySet())

@Serializable
data class Flag(val value: Boolean)

/** Public configuration the browser needs before it can start (the server reads it from env/.env). */
@Serializable
data class ClientConfig(
    val supabaseUrl: String? = null,
    val supabaseAnonKey: String? = null,
)

@Serializable
data class ServerError(val message: String)

/** Credentials travel in a POST body, never in a URL. The upstream host is fixed by the server. */
@Serializable
data class ZoteroFileRequest(
    val apiKey: String,
    val userId: String,
    val attachmentKey: String,
    val groupId: String? = null,
)
