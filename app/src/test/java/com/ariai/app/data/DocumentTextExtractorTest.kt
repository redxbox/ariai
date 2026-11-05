package com.ariai.app.data

import com.ariai.app.util.extractDocxText
import com.ariai.app.util.pdfPageCountToExtract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentTextExtractorTest {
    @Test
    fun detectsPdfAndDocxByMimeTypeOrFilename() {
        assertTrue(DocumentTextExtractor.isSupportedDocument("application/pdf", "report.bin"))
        assertTrue(DocumentTextExtractor.isSupportedDocument("application/octet-stream", "report.PDF"))
        assertTrue(
            DocumentTextExtractor.isSupportedDocument(
                DocumentTextExtractor.DOCX_MIME_TYPE,
                "document.bin"
            )
        )
        assertTrue(DocumentTextExtractor.isSupportedDocument("application/octet-stream", "document.docx"))
        assertFalse(DocumentTextExtractor.isSupportedDocument("text/plain", "notes.txt"))
    }

    @Test
    fun extractsParagraphsAndTableCellsFromDocxZipInJvmUnitTest() {
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

        val extracted = extractDocxText(ByteArrayInputStream(archive)) { KXmlParser() }

        assertEquals("Hello & world\nSecond\tline\nFruit\tCount\nApple\t3", extracted)
    }

    @Test
    fun pdfPageLimitUsesOnlyTheConfiguredLeadingPages() {
        assertEquals(20, pdfPageCountToExtract(240))
        assertEquals(12, pdfPageCountToExtract(240, maximumPages = 12))
        assertEquals(0, pdfPageCountToExtract(-1))
        assertEquals(0, pdfPageCountToExtract(10, maximumPages = -4))
    }

}
