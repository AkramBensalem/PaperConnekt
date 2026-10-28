package com.paperconnekt.secrets

class NoOpSecretStore : SecretStore {
    override suspend fun read(key: SecretKey): String? = null

    override suspend fun write(key: SecretKey, value: String) = Unit

    override suspend fun delete(key: SecretKey) = Unit
}

