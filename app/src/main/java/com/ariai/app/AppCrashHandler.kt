package com.ariai.app

import android.app.Application
import android.os.Process
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import kotlin.system.exitProcess

/** Persists the most recent uncaught exception and then delegates to Android's existing handler. */
internal class AppCrashHandler(
    private val reportFile: File,
    private val previousHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        persistReport(thread, throwable)

        val delegate = previousHandler
        if (delegate != null && delegate !== this) {
            try {
                delegate.uncaughtException(thread, throwable)
                return
            } catch (_: Throwable) {
                // If a custom handler itself fails, still terminate rather than leaving a corrupt process.
            }
        }
        terminateProcess()
    }

    @Synchronized
    private fun persistReport(thread: Thread, throwable: Throwable) {
        try {
            val parent = reportFile.parentFile ?: return
            if (!parent.exists() && !parent.mkdirs()) return
            val temporaryFile = File(parent, "${reportFile.name}.tmp")
            FileOutputStream(temporaryFile).bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                val header = buildString {
                    appendLine("AriAI crash report")
                    appendLine("Time: ${System.currentTimeMillis()}")
                    appendLine("Thread: ${thread.name.take(MAX_THREAD_NAME_CHARS)}")
                    appendLine()
                }.take(MAX_REPORT_CHARS)
                writer.write(header)
                val limitedWriter = LimitedWriter(writer, MAX_REPORT_CHARS - header.length)
                val traceWriter = PrintWriter(limitedWriter)
                throwable.printStackTrace(traceWriter)
                traceWriter.flush()
                writer.flush()
            }

            if (!temporaryFile.renameTo(reportFile)) {
                FileInputStream(temporaryFile).use { input ->
                    FileOutputStream(reportFile).use { output -> input.copyTo(output) }
                }
                temporaryFile.delete()
            }
        } catch (_: Throwable) {
            // A crash handler must still chain to the platform handler if storage is unavailable.
        }
    }

    private fun terminateProcess(): Nothing {
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    private class LimitedWriter(
        private val target: Writer,
        private var remaining: Int
    ) : Writer() {
        override fun write(buffer: CharArray, offset: Int, length: Int) {
            val allowed = length.coerceAtMost(remaining).coerceAtLeast(0)
            if (allowed > 0) {
                target.write(buffer, offset, allowed)
                remaining -= allowed
            }
        }

        override fun write(value: String, offset: Int, length: Int) {
            val allowed = length.coerceAtMost(remaining).coerceAtLeast(0)
            if (allowed > 0) {
                target.write(value, offset, allowed)
                remaining -= allowed
            }
        }

        override fun flush() = target.flush()

        // PrintWriter closes its wrapper; the owning buffered writer closes the underlying file.
        override fun close() = flush()
    }

    companion object {
        const val REPORT_FILE_NAME = "crash-report.txt"
        internal const val MAX_REPORT_CHARS = 64 * 1024
        private const val MAX_THREAD_NAME_CHARS = 200
        private const val MAX_REPORT_BYTES = MAX_REPORT_CHARS * 4

        fun install(application: Application) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                AppCrashHandler(File(application.filesDir, REPORT_FILE_NAME), previous)
            )
        }

        fun readReport(filesDirectory: File): String? {
            return try {
                val file = File(filesDirectory, REPORT_FILE_NAME)
                if (!file.isFile) return null
                val bytes = ByteArray(MAX_REPORT_BYTES)
                var count = 0
                FileInputStream(file).use { input ->
                    while (count < bytes.size) {
                        val read = input.read(bytes, count, bytes.size - count)
                        if (read <= 0) break
                        count += read
                    }
                }
                String(bytes, 0, count, StandardCharsets.UTF_8).trim().ifBlank { null }
            } catch (_: Exception) {
                null
            }
        }

        fun clearReport(filesDirectory: File): Boolean {
            val report = File(filesDirectory, REPORT_FILE_NAME)
            val temporary = File(filesDirectory, "$REPORT_FILE_NAME.tmp")
            val reportRemoved = !report.exists() || report.delete()
            val temporaryRemoved = !temporary.exists() || temporary.delete()
            return reportRemoved && temporaryRemoved
        }
    }
}
