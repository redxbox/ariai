package com.ariai.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

object CrashHandler {
    private const val TAG = "AriAiCrash"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
                // Save crash log
                saveCrashLog(context, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            } finally {
                // Call default handler to show system crash dialog or just kill
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(context: Context, throwable: Throwable) {
        try {
            val crashLog = """
                Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                Thread: ${Thread.currentThread().name}
                Exception: ${throwable.javaClass.name}
                Message: ${throwable.message}
                StackTrace:
                ${throwable.stackTraceToString()}
                
                Cause: ${throwable.cause?.stackTraceToString() ?: "None"}
            """.trimIndent()
            
            context.openFileOutput("crash_log.txt", Context.MODE_APPEND).use { output ->
                output.write("\n\n--- CRASH ---\n".toByteArray())
                output.write(crashLog.toByteArray())
            }
        } catch (e: Exception) {}
    }

    fun getCoroutineExceptionHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Coroutine exception", throwable)
        }
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
        } catch (e: Exception) {}
    }
}
