package com.ariai.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class AppCrashHandlerTest {
    @Test
    fun storesBoundedStackTraceThenChainsToPreviousHandlerAndCanClearIt() {
        val directory = Files.createTempDirectory("ariai-crash-test").toFile()
        var chainedThread: Thread? = null
        var chainedError: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { thread, error ->
            chainedThread = thread
            chainedError = error
        }
        val handler = AppCrashHandler(File(directory, AppCrashHandler.REPORT_FILE_NAME), previous)
        val thread = Thread("failing-worker")
        val failure = IllegalStateException("crash-marker " + "x".repeat(AppCrashHandler.MAX_REPORT_CHARS * 2))

        try {
            handler.uncaughtException(thread, failure)

            val report = AppCrashHandler.readReport(directory)
            assertNotNull(report)
            assertTrue(report!!.contains("AriAI crash report"))
            assertTrue(report.contains("failing-worker"))
            assertTrue(report.contains("crash-marker"))
            assertTrue(report.length <= AppCrashHandler.MAX_REPORT_CHARS)
            assertEquals(thread, chainedThread)
            assertEquals(failure, chainedError)

            assertTrue(AppCrashHandler.clearReport(directory))
            assertNull(AppCrashHandler.readReport(directory))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun clearIsSafeWhenNoCrashReportExists() {
        val directory = Files.createTempDirectory("ariai-crash-empty").toFile()
        try {
            assertFalse(File(directory, AppCrashHandler.REPORT_FILE_NAME).exists())
            assertTrue(AppCrashHandler.clearReport(directory))
            assertNull(AppCrashHandler.readReport(directory))
        } finally {
            directory.deleteRecursively()
        }
    }
}
