package com.ariai.app.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ariai.app.data.models.Provider
import com.ariai.app.data.models.ProviderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QRCodeUtilTest {
    @Test
    fun generatedProviderQrCanBeDecodedAndImported() {
        val provider = Provider(
            name = "QR import test",
            type = ProviderType.OPENAI_COMPATIBLE,
            baseUrl = "https://example.invalid/v1",
            apiKey = "not-a-real-key",
            customHeaders = mapOf("X-Test-Header" to "qr-import")
        )
        val payload = QRCodeUtil.providerToJson(provider)
        val bitmap = QRCodeUtil.generateQRCode(payload, size = 640)
        assertNotNull("provider QR should be generated", bitmap)
        bitmap ?: return

        try {
            val decoded = QRCodeUtil.decodeQRCode(bitmap)
            assertEquals("the gallery QR decoder should recover the complete payload", payload, decoded)

            val imported = QRCodeUtil.jsonToProvider(decoded.orEmpty())
            assertNotNull("decoded provider JSON should be importable", imported)
            assertEquals(provider.name, imported?.name)
            assertEquals(provider.type, imported?.type)
            assertEquals(provider.baseUrl, imported?.baseUrl)
            assertEquals(provider.apiKey, imported?.apiKey)
            assertEquals(provider.customHeaders, imported?.customHeaders)
        } finally {
            bitmap.recycle()
        }
    }
}
