package com.akram.bensalem.paper.connekt.shared.zotero.exception

import io.ktor.http.HttpStatusCode

class CrossRefException(message: String, status: HttpStatusCode) : Exception(message)