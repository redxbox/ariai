package com.ariai.app.util

import android.graphics.Bitmap
import android.graphics.Matrix
import com.ariai.app.data.models.Provider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

object QRCodeUtil {
    fun providerToJson(provider: Provider): String {
        return JSONObject().apply {
            put("name", provider.name)
            put("type", provider.type.name)
            put("baseUrl", provider.baseUrl)
            put("apiKey", provider.apiKey)
            put("customHeaders", JSONObject(provider.customHeaders))
        }.toString()
    }

    fun jsonToProvider(jsonStr: String): Provider? {
        return try {
            val json = JSONObject(jsonStr)
            Provider(
                name = json.getString("name"),
                type = com.ariai.app.data.models.ProviderType.valueOf(json.getString("type")),
                baseUrl = json.getString("baseUrl"),
                apiKey = json.getString("apiKey"),
                customHeaders = json.optJSONObject("customHeaders")?.let { obj ->
                    val map = mutableMapOf<String, String>()
                    obj.keys().forEach { key ->
                        map[key] = obj.getString(key)
                    }
                    map
                } ?: emptyMap()
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Decodes a QR code from an image bitmap, trying common camera/gallery rotations. */
    fun decodeQRCode(bitmap: Bitmap): String? {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        for (degrees in listOf(0f, 90f, 180f, 270f)) {
            val candidate = if (degrees == 0f) bitmap else runCatching {
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    Matrix().apply { postRotate(degrees) },
                    true
                )
            }.getOrNull() ?: continue
            try {
                val pixels = IntArray(candidate.width * candidate.height)
                candidate.getPixels(pixels, 0, candidate.width, 0, 0, candidate.width, candidate.height)
                val source = RGBLuminanceSource(candidate.width, candidate.height, pixels)
                val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), hints)
                return result.text
            } catch (_: Exception) {
                // The image may be rotated or may not contain a QR code; try the next orientation.
            } finally {
                if (candidate !== bitmap) candidate.recycle()
            }
        }
        return null
    }

    fun generateQRCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
