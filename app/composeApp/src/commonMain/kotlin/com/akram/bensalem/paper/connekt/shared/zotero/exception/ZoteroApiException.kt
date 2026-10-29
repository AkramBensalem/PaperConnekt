package com.akram.bensalem.paper.connekt.shared.zotero.exception

import io.ktor.http.HttpStatusCode

class ZoteroApiException(message: String, val status: HttpStatusCode) : Exception(message) {
    val statusCode: Int = status.value
}