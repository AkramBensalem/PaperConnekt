package com.paperconnekt.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey as JceSecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEY_ALIAS = "paperconnekt-secrets"
private const val PREFERENCES_NAME = "paperconnekt-secrets"

private var applicationContext: Context? = null

actual fun configureSecretStore(platformContext: Any?) {
    applicationContext = (platformContext as? Context)?.applicationContext
        ?: error("Android Context is required to configure secret storage.")
}

actual fun createSecretStore(): SecretStore {
    return AndroidSecretStore(
        context = requireNotNull(applicationContext) {
            "Call configureSecretStore before creating the secret store."
        },
    )
}

private class AndroidSecretStore(
    private val context: Context,
) : SecretStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun read(key: SecretKey): String? {
        val encoded = preferences.getString(key.name, null) ?: return null
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, 12)
        val encrypted = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            loadKey(),
            GCMParameterSpec(128, iv),
        )
        return cipher.doFinal(encrypted).decodeToString()
    }

    override suspend fun write(key: SecretKey, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, loadKey())
        val payload = cipher.iv + cipher.doFinal(value.encodeToByteArray())
        preferences.edit()
            .putString(key.name, Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    override suspend fun delete(key: SecretKey) {
        preferences.edit().remove(key.name).apply()
    }

    private fun loadKey(): JceSecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? JceSecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }
}
