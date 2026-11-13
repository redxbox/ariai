package com.ariai.app.util

import android.util.Log

/** Records errors that are safe to ignore for the user, so they stay visible to developers. */
object AppLog {
    fun warn(e: Throwable, where: String = "AriAI") {
        // Logging must never fail the caller (unit tests run without Android's Log implementation).
        runCatching { Log.w("AriAI", where, e) }
    }
}
