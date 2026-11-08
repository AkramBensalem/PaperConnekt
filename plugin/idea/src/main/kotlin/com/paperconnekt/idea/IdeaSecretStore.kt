package com.paperconnekt.idea

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class IdeaSecretStore : SecretStore {
    private fun attributes(key: SecretKey) = CredentialAttributes("PaperConnekt contest/${key.name}")
    override suspend fun read(key: SecretKey): String? = withContext(Dispatchers.IO) {
        PasswordSafe.instance.getPassword(attributes(key))
    }
    override suspend fun write(key: SecretKey, value: String) = withContext(Dispatchers.IO) {
        PasswordSafe.instance.setPassword(attributes(key), value)
    }
    override suspend fun delete(key: SecretKey) = withContext(Dispatchers.IO) {
        PasswordSafe.instance.setPassword(attributes(key), null)
    }
}
