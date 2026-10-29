package com.akram.bensalem.paper.connekt.feature.assistant

import com.akram.bensalem.paper.connekt.shared.zotero.exception.ZoteroApiException
import io.ktor.client.plugins.ResponseException

internal fun providerFailureMessage(exception: Throwable, provider: String): String? {
    val causes = generateSequence(exception) { it.cause }.take(8).toList()
    val status = causes.firstNotNullOfOrNull { cause ->
        when (cause) {
            is ZoteroApiException -> cause.statusCode
            is ResponseException -> cause.response.status.value
            else -> null
        }
    }

    if (status != null) {
        return when (status) {
            401, 403 -> "$provider rejected this API key or it lacks permission. Update the key in Settings → API keys."
            402 -> "$provider requires an active account or available credit for this request."
            413 -> "This request is too large for $provider. Reduce the amount of content and try again."
            429 -> "$provider is receiving too many requests. Wait a moment and try again."
            in 500..599 -> "$provider is temporarily unavailable. Try again later."
            else -> null
        }
    }

    val networkFailure = causes.any { cause ->
        val details = "${cause::class.simpleName.orEmpty()} ${cause.message.orEmpty()}".lowercase()
        listOf(
            "unknownhost",
            "unresolvedaddress",
            "connectexception",
            "socketexception",
            "connection refused",
            "connection reset",
            "no route to host",
            "network is unreachable",
            "network unavailable",
            "unable to resolve host",
            "unable to resolve address",
            "failed to connect",
            "failed to establish",
            "timed out",
            "timeout",
        ).any(details::contains)
    }
    return if (networkFailure) {
        "Could not connect to $provider. Check your internet connection and try again."
    } else {
        null
    }
}
