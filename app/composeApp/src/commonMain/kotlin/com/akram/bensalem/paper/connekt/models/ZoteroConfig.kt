package com.akram.bensalem.paper.connekt.models

/**
 * Basic configuration needed to call the Zotero Web API.
 *
 * @param apiKey Your Zotero API key (scoped to the target library).
 * @param userId Numeric Zotero user ID whose library to access.
 * @param baseUrl Base API URL; defaults to the official public API.
 * @param groupId Optional numeric group ID; otherwise the user's personal library is used.
 */
data class ZoteroConfig(
    val apiKey: String,
    val userId: String,
    private val baseUrl: String = BASE_URL,
    val groupId: String? = null
){

    companion object {
        const val BASE_URL = "https://api.zotero.org"
    }

    val base
        get() = groupId?.let { "${baseUrl.trimEnd('/')}/groups/$it" } ?: userBase

    val userBase
        get() = "${baseUrl.trimEnd('/')}/users/$userId"

}
