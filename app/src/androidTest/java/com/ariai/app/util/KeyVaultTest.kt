package com.ariai.app.util

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyVaultTest {
    @Test
    fun encryptedKeysRoundTripAndUnreadableCiphertextIsNotTreatedAsAValidEmptyKey() {
        val secret = "test-provider-key"
        val encrypted = KeyVault.encrypt(secret)

        assertEquals(secret, KeyVault.decryptOrNull(encrypted))
        assertNull(KeyVault.decryptOrNull("enc1:${Base64.encodeToString(ByteArray(32), Base64.NO_WRAP)}"))
    }

    @Test
    fun legacyPlainTextKeysRemainReadable() {
        assertEquals("legacy-key", KeyVault.decryptOrNull("legacy-key"))
    }
}
