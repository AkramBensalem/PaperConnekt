package com.akram.bensalem.paper.connekt.core.remote.util

sealed class RemoteResult<out T> {
    data class Success<T>(val data: T) : RemoteResult<T>()
    data class Error(val exception: RemoteException) : RemoteResult<Nothing>()
}

inline fun <T> RemoteResult<T>.onSuccess(action: (T) -> Unit): RemoteResult<T> {
    if (this is RemoteResult.Success) action(data)
    return this
}

inline fun <T> RemoteResult<T>.onError(action: (RemoteException) -> Unit): RemoteResult<T> {
    if (this is RemoteResult.Error) action(exception)
    return this
}

fun <T> RemoteResult<T>.getOrThrow(): T = when (this) {
    is RemoteResult.Success -> data
    is RemoteResult.Error -> throw exception
}
