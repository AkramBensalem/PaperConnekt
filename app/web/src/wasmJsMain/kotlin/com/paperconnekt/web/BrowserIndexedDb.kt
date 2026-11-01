package com.paperconnekt.web

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val databaseName = "paperconnekt"

internal object BrowserIndexedDb {
    suspend fun read(key: String): String? = suspendCancellableCoroutine { continuation ->
        readValue(
            key = key,
            onSuccess = { value -> if (continuation.isActive) continuation.resume(value) },
            onError = { message ->
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
            },
        )
    }

    suspend fun write(key: String, value: String) = suspendCancellableCoroutine<Unit> { continuation ->
        writeValue(
            key = key,
            value = value,
            onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
            onError = { message ->
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
            },
        )
    }

    suspend fun move(key: String, replacementKey: String) = suspendCancellableCoroutine<Unit> { continuation ->
        moveValue(
            key = key,
            replacementKey = replacementKey,
            onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
            onError = { message ->
                if (continuation.isActive) continuation.resumeWithException(IllegalStateException(message))
            },
        )
    }
}

private fun readValue(
    key: String,
    onSuccess: (String?) -> Unit,
    onError: (String) -> Unit,
): Unit = js(
    """
    {
        const open = indexedDB.open("$databaseName", 1);
        open.onupgradeneeded = function() {
            const db = open.result;
            if (!db.objectStoreNames.contains("values")) db.createObjectStore("values");
        };
        open.onerror = function() {
            onError(open.error ? open.error.message : "Could not open browser storage.");
        };
        open.onsuccess = function() {
            const db = open.result;
            const request = db.transaction("values", "readonly").objectStore("values").get(key);
            request.onerror = function() {
                onError(request.error ? request.error.message : "Could not read browser storage.");
                db.close();
            };
            request.onsuccess = function() {
                onSuccess(request.result == null ? null : String(request.result));
                db.close();
            };
        };
    }
    """,
)

private fun writeValue(
    key: String,
    value: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
): Unit = js(
    """
    {
        const open = indexedDB.open("$databaseName", 1);
        open.onupgradeneeded = function() {
            const db = open.result;
            if (!db.objectStoreNames.contains("values")) db.createObjectStore("values");
        };
        open.onerror = function() {
            onError(open.error ? open.error.message : "Could not open browser storage.");
        };
        open.onsuccess = function() {
            const db = open.result;
            const request = db.transaction("values", "readwrite").objectStore("values").put(value, key);
            request.onerror = function() {
                onError(request.error ? request.error.message : "Could not write browser storage.");
                db.close();
            };
            request.onsuccess = function() {
                onSuccess();
                db.close();
            };
        };
    }
    """,
)

private fun moveValue(
    key: String,
    replacementKey: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
): Unit = js(
    """
    {
        const open = indexedDB.open("$databaseName", 1);
        open.onupgradeneeded = function() {
            const db = open.result;
            if (!db.objectStoreNames.contains("values")) db.createObjectStore("values");
        };
        open.onerror = function() {
            onError(open.error ? open.error.message : "Could not open browser storage.");
        };
        open.onsuccess = function() {
            const db = open.result;
            const transaction = db.transaction("values", "readwrite");
            const store = transaction.objectStore("values");
            const request = store.get(key);
            request.onerror = function() {
                onError(request.error ? request.error.message : "Could not preserve browser storage.");
                db.close();
            };
            request.onsuccess = function() {
                if (request.result != null) store.put(request.result, replacementKey);
                store.delete(key);
            };
            transaction.oncomplete = function() {
                onSuccess();
                db.close();
            };
            transaction.onerror = function() {
                onError(transaction.error ? transaction.error.message : "Could not preserve browser storage.");
                db.close();
            };
        };
    }
    """,
)
