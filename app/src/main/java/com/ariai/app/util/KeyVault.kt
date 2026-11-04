package com.ariai.app.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts API keys before they are written to disk. The AES key lives in the Android Keystore,
 * so the database and preference files hold only ciphertext. Values without the prefix are
 * legacy plain text; callers migrate those values after a successful read.
 */
object KeyVault {
    private const val ALIAS = "ariai_key_vault"
    private const val PREFIX = "enc1:"
    private const val IV_BYTES = 12

    private fun secretKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val data = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(cipher.iv + data, Base64.NO_WRAP)
        } catch (e: Exception) {
            AppLog.warn(e, "KeyVault.encrypt")
            throw IllegalStateException("Could not encrypt API key with Android Keystore", e)
        }
    }

    /**
     * Returns null when encrypted data cannot be opened. Unlike encrypt(), decrypt must not create a
     * replacement Keystore key: doing so would silently turn a lost key into an empty API key and a
     * misleading HTTP 401.
     */
    fun decryptOrNull(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            val raw = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            require(raw.size > IV_BYTES) { "Encrypted value is truncated" }
            val iv = raw.copyOfRange(0, IV_BYTES)
            val data = raw.copyOfRange(IV_BYTES, raw.size)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key = (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
                ?: throw IllegalStateException("Android Keystore key is missing")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(data), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLog.warn(e, "KeyVault.decrypt")
            null
        }
    }

    /** Backward-compatible best-effort accessor for preferences that can be re-entered separately. */
    fun decrypt(stored: String): String = decryptOrNull(stored).orEmpty()
}
