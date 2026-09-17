package com.ariai.app.util

import android.graphics.Bitmap
import com.ariai.app.data.models.Provider
import com.google.zxing.BarcodeFormat
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
