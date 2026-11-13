package com.ariai.app.util

import java.util.Calendar

/**
 * Calendar-day grouping for chat history rows. [PINNED] is display-only — it never comes out of
 * the clock; [chatDateBucket] only ever returns the time-based buckets.
 */
enum class ChatDateBucket(val labelKey: String) {
    PINNED("Pinned"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    PREVIOUS_7_DAYS("Previous 7 days"),
    EARLIER("Earlier")
}

/** Buckets a message/chat timestamp by calendar day (today, yesterday, previous 7 days, earlier). */
fun chatDateBucket(timestamp: Long, now: Long = System.currentTimeMillis()): ChatDateBucket {
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val previousWeekStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
    return when {
        timestamp >= todayStart.timeInMillis -> ChatDateBucket.TODAY
        timestamp >= yesterdayStart.timeInMillis -> ChatDateBucket.YESTERDAY
        timestamp >= previousWeekStart.timeInMillis -> ChatDateBucket.PREVIOUS_7_DAYS
        else -> ChatDateBucket.EARLIER
    }
}
