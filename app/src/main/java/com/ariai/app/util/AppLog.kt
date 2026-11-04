package com.ariai.app.util

import android.util.Log

/** Records errors that are safe to ignore for the user, so they stay visible to developers. */
object AppLog {
    fun warn(e: Throwable, where: String = "AriAI") {
        Log.w("AriAI", where, e)
    }
}
