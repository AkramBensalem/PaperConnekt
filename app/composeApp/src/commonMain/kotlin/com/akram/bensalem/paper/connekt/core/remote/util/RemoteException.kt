package com.akram.bensalem.paper.connekt.core.remote.util

class RemoteException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null,
) : Exception(message, cause)
