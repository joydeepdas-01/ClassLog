package com.classlog.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelTimePickerTest {

    @Test
    fun to12Hour_midnight_1200AM() {
        val (h, m, isPm) = WheelTimeConverter.to12Hour(0)
        assertEquals(12, h)
        assertEquals(0, m)
        assertEquals(false, isPm)
    }

    @Test
    fun to12Hour_morning_0930AM() {
        val (h, m, isPm) = WheelTimeConverter.to12Hour(9 * 60 + 30)
        assertEquals(9, h)
        assertEquals(30, m)
        assertEquals(false, isPm)
    }

    @Test
    fun to12Hour_noon_1200PM() {
        val (h, m, isPm) = WheelTimeConverter.to12Hour(12 * 60)
        assertEquals(12, h)
        assertEquals(0, m)
        assertEquals(true, isPm)
    }

    @Test
    fun to12Hour_afternoon_0145PM() {
        val (h, m, isPm) = WheelTimeConverter.to12Hour(13 * 60 + 45)
        assertEquals(1, h)
        assertEquals(45, m)
        assertEquals(true, isPm)
    }

    @Test
    fun to12Hour_night_1159PM() {
        val (h, m, isPm) = WheelTimeConverter.to12Hour(23 * 60 + 59)
        assertEquals(11, h)
        assertEquals(59, m)
        assertEquals(true, isPm)
    }

    @Test
    fun toMinutes_midnight_1200AM() {
        val minutes = WheelTimeConverter.toMinutes(hour12 = 12, minute = 0, isPm = false)
        assertEquals(0, minutes)
    }

    @Test
    fun toMinutes_noon_1200PM() {
        val minutes = WheelTimeConverter.toMinutes(hour12 = 12, minute = 0, isPm = true)
        assertEquals(720, minutes)
    }

    @Test
    fun toMinutes_afternoon_0315PM() {
        val minutes = WheelTimeConverter.toMinutes(hour12 = 3, minute = 15, isPm = true)
        assertEquals(15 * 60 + 15, minutes)
    }

    @Test
    fun toMinutes_night_1159PM() {
        val minutes = WheelTimeConverter.toMinutes(hour12 = 11, minute = 59, isPm = true)
        assertEquals(23 * 60 + 59, minutes)
    }

    @Test
    fun roundtrip_allMinutes_0to1439() {
        for (m in 0..1439) {
            val (h12, min, isPm) = WheelTimeConverter.to12Hour(m)
            val converted = WheelTimeConverter.toMinutes(h12, min, isPm)
            assertEquals("Mismatch for minute $m", m, converted)
        }
    }

    @Test
    fun toMinutes24_boundaries() {
        assertEquals(0, WheelTimeConverter.toMinutes24(0, 0))
        assertEquals(23 * 60 + 59, WheelTimeConverter.toMinutes24(23, 59))
        assertEquals(12 * 60, WheelTimeConverter.toMinutes24(12, 0))
    }

    @Test
    fun circularCycling_hourForwardAndReverse() {
        val hours = (1..12).toList()
        val count = hours.size

        // Forward: 11 -> 12 -> 1 -> 2
        val idx11 = 10 // item 11
        assertEquals(11, hours[Math.floorMod(idx11, count)])
        assertEquals(12, hours[Math.floorMod(idx11 + 1, count)])
        assertEquals(1, hours[Math.floorMod(idx11 + 2, count)])
        assertEquals(2, hours[Math.floorMod(idx11 + 3, count)])

        // Reverse: 2 -> 1 -> 12 -> 11
        val idx2 = 1 // item 2
        assertEquals(2, hours[Math.floorMod(idx2, count)])
        assertEquals(1, hours[Math.floorMod(idx2 - 1, count)])
        assertEquals(12, hours[Math.floorMod(idx2 - 2, count)])
        assertEquals(11, hours[Math.floorMod(idx2 - 3, count)])
    }

    @Test
    fun circularCycling_minuteForwardAndReverse() {
        val count = 60

        // Forward: 58 -> 59 -> 00 -> 01
        assertEquals(58, Math.floorMod(58, count))
        assertEquals(59, Math.floorMod(59, count))
        assertEquals(0, Math.floorMod(60, count))
        assertEquals(1, Math.floorMod(61, count))

        // Reverse: 01 -> 00 -> 59 -> 58
        assertEquals(1, Math.floorMod(1, count))
        assertEquals(0, Math.floorMod(0, count))
        assertEquals(59, Math.floorMod(-1, count))
        assertEquals(58, Math.floorMod(-2, count))
    }

    @Test
    fun circularCycling_amPmForwardAndReverse() {
        val periods = listOf("AM", "PM")
        val count = 2

        // Forward: AM -> PM -> AM -> PM
        assertEquals("AM", periods[Math.floorMod(0, count)])
        assertEquals("PM", periods[Math.floorMod(1, count)])
        assertEquals("AM", periods[Math.floorMod(2, count)])
        assertEquals("PM", periods[Math.floorMod(3, count)])

        // Reverse: PM -> AM -> PM -> AM
        assertEquals("PM", periods[Math.floorMod(1, count)])
        assertEquals("AM", periods[Math.floorMod(0, count)])
        assertEquals("PM", periods[Math.floorMod(-1, count)])
        assertEquals("AM", periods[Math.floorMod(-2, count)])
    }
}
