package com.paperconnekt.secrets

interface SecretStore {
    suspend fun read(key: SecretKey): String?
    suspend fun write(key: SecretKey, value: String)
    suspend fun delete(key: SecretKey)
}

