package com.paperconnekt.web

import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore

class BrowserSecretStore : SecretStore {
    override suspend fun read(key: SecretKey): String? =
        BrowserIndexedDb.read(storageKey(key))?.takeIf(String::isNotEmpty)

    override suspend fun write(key: SecretKey, value: String) {
        BrowserIndexedDb.write(storageKey(key), value)
    }

    override suspend fun delete(key: SecretKey) {
        BrowserIndexedDb.write(storageKey(key), "")
    }

    private fun storageKey(key: SecretKey): String = "secret:${key.name}"
}
