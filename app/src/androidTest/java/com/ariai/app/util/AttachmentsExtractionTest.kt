package com.ariai.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class AttachmentsExtractionTest {
    @Test
    fun extractsSelectablePdfText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PDFBoxResourceLoader.init(context)
        val pdf = File.createTempFile("ariai-extract-", ".pdf", context.cacheDir)
        try {
            PDDocument().use { document ->
                val page = PDPage()
                document.addPage(page)
                PDPageContentStream(document, page).use { stream ->
                    stream.beginText()
                    stream.setFont(PDType1Font.HELVETICA, 12f)
                    stream.newLineAtOffset(72f, 720f)
                    stream.showText("AriAI PDF extraction")
                    stream.endText()
                }
                document.save(pdf)
            }

            assertEquals("AriAI PDF extraction", extractPdfTextFromFile(context, pdf))
        } finally {
            pdf.delete()
        }
    }

    @Test
    fun performsOfflineOcrForScannedPdfPages() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PDFBoxResourceLoader.init(context)
        val bitmap = Bitmap.createBitmap(1224, 1584, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 116f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawText("OFFLINE OCR 42", 90f, 820f, paint)
        val png = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.toByteArray()
        }
        bitmap.recycle()

        val pdf = File.createTempFile("ariai-ocr-", ".pdf", context.cacheDir)
        try {
            PDDocument().use { document ->
                val page = PDPage()
                document.addPage(page)
                val image = PDImageXObject.createFromByteArray(document, png, "scanned-page")
                PDPageContentStream(document, page).use { stream ->
                    stream.drawImage(image, 0f, 0f, 612f, 792f)
                }
                document.save(pdf)
            }

            val extracted = extractPdfTextFromFile(context, pdf)
            assertTrue("Expected OCR text in: $extracted", extracted.contains("OCR", ignoreCase = true))
        } finally {
            pdf.delete()
        }
    }

    @Test
    fun extractsWordParagraphsAndInlineBreaksFromDocumentXml() {
        val documentXml = """<?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Hello &amp; world</w:t></w:r></w:p>
                <w:p><w:r><w:t>Second</w:t><w:tab/><w:t>line</w:t></w:r></w:p>
                <w:tbl>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Fruit</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>Count</w:t></w:r></w:p></w:tc>
                  </w:tr>
                  <w:tr>
                    <w:tc><w:p><w:r><w:t>Apple</w:t></w:r></w:p></w:tc>
                    <w:tc><w:p><w:r><w:t>3</w:t></w:r></w:p></w:tc>
                  </w:tr>
                </w:tbl>
              </w:body>
            </w:document>
        """.trimIndent()
        val archive = ByteArrayOutputStream().use { output ->
            ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
                zip.putNextEntry(ZipEntry("[Content_Types].xml"))
                zip.write("<Types/>".toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("word/document.xml"))
                zip.write(documentXml.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
            output.toByteArray()
        }

        assertEquals(
            "Hello & world\nSecond\tline\nFruit\tCount\nApple\t3",
            extractDocxText(ByteArrayInputStream(archive))
        )
    }
}
