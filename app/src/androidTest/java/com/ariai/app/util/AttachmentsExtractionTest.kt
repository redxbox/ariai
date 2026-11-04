package com.ariai.app.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
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
    fun extractsWordParagraphsAndInlineBreaksFromDocumentXml() {
        val documentXml = """<?xml version="1.0" encoding="UTF-8"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                <w:p><w:r><w:t>Hello &amp; world</w:t></w:r></w:p>
                <w:p><w:r><w:t>Second</w:t><w:tab/><w:t>line</w:t></w:r></w:p>
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
            "Hello & world\nSecond\tline",
            extractDocxText(ByteArrayInputStream(archive))
        )
    }
}
