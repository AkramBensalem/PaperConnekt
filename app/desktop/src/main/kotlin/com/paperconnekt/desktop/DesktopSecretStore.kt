package com.paperconnekt.desktop

import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import java.util.prefs.Preferences

class DesktopSecretStore : SecretStore {
    private val preferences = Preferences.userRoot().node("com.paperconnekt.secrets")

    override suspend fun read(key: SecretKey): String? {
        return preferences.get(key.name, null)
    }

    override suspend fun write(key: SecretKey, value: String) {
        preferences.put(key.name, value)
        preferences.flush()
    }

    override suspend fun delete(key: SecretKey) {
        preferences.remove(key.name)
        preferences.flush()
    }
}
