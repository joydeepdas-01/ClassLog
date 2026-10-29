package com.classlog.app.domain.time

/**
 * Pure Kotlin formatter for durations and clock times.
 * Avoids Android or device time-zone quirks by operating directly on minute offsets.
 */
object TimeFormat {
    /**
     * Formats duration in minutes to human-readable strings (e.g., "2h", "1h 30m", "45m").
     */
    fun duration(minutes: Int): String = when {
        minutes % 60 == 0 -> "${minutes / 60}h"
        minutes < 60 -> "${minutes}m"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    /**
     * Formats minute offset from midnight to 12-hour (e.g., "10:00 AM", "12:00 PM")
     * or 24-hour (e.g., "13:05") representation.
     */
    fun clock(minutes: Int, use24h: Boolean): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        if (use24h) return "%02d:%02d".format(h, m)
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, m, if (h < 12) "AM" else "PM")
    }
}
