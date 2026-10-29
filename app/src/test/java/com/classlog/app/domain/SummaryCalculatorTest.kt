package com.classlog.app.domain

import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.domain.calculator.SummaryCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SummaryCalculatorTest {
    private fun s(day: Long, dur: Int, cat: String, teach: Boolean, batch: String? = null) =
        WorkSessionEntity(0, day, 600, 600 + dur, dur, 1, null, cat, batch, teach, "", 0, 0)

    @Test
    fun emptyMonth_isAllZero() {
        val r = SummaryCalculator.summarise(emptyList())
        assertEquals(0, r.totalMinutes)
        assertEquals(0, r.daysWorked)
    }

    @Test
    fun totalsAndDays() {
        val r = SummaryCalculator.summarise(
            listOf(
                s(1, 60, "Teaching", true, "Kids"),
                s(1, 120, "Teaching", true, "XII"),
                s(2, 30, "Office Work", false)
            )
        )
        assertEquals(210, r.totalMinutes)
        assertEquals(3, r.sessionCount)
        assertEquals(2, r.daysWorked)
    }

    @Test
    fun teachingVsNonTeaching() {
        val r = SummaryCalculator.summarise(
            listOf(
                s(1, 60, "Teaching", true, "Kids"),
                s(1, 45, "Maintenance", false)
            )
        )
        assertEquals(60, r.teachingMinutes)
        assertEquals(45, r.nonTeachingMinutes)
    }

    @Test
    fun groupsByBatchAndCategory() {
        val r = SummaryCalculator.summarise(
            listOf(
                s(1, 60, "Teaching", true, "Kids"),
                s(2, 60, "Teaching", true, "Kids"),
                s(2, 30, "Billing", false)
            )
        )
        assertEquals(120, r.byBatch["Kids"])
        assertEquals(30, r.byCategory["Billing"])
        assertNull(r.byBatch["Billing"])
    }
}
