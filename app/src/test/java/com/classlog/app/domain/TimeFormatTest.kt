package com.classlog.app.domain

import com.classlog.app.domain.time.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun wholeHours() = assertEquals("2h", TimeFormat.duration(120))

    @Test
    fun hoursAndMinutes() = assertEquals("1h 30m", TimeFormat.duration(90))

    @Test
    fun minutesOnly() = assertEquals("45m", TimeFormat.duration(45))

    @Test
    fun clock12h_morning() = assertEquals("10:00 AM", TimeFormat.clock(600, false))

    @Test
    fun clock12h_noon() = assertEquals("12:00 PM", TimeFormat.clock(720, false))

    @Test
    fun clock12h_midnightHour() = assertEquals("12:30 AM", TimeFormat.clock(30, false))

    @Test
    fun clock24h() = assertEquals("13:05", TimeFormat.clock(785, true))
}
