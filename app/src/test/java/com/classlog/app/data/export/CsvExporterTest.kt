package com.classlog.app.data.export

import com.classlog.app.data.local.entity.WorkSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {

    @Test
    fun generateCsv_emptyList_generatesHeadersOnly() {
        val csv = CsvExporter.generateCsv(emptyList(), use24Hour = false)
        val lines = csv.trim().split("\r\n")
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("Date,Start Time,End Time"))
    }

    @Test
    fun generateCsv_validSessions_generatesRowsAndSummary() {
        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        val sessions = listOf(
            WorkSessionEntity(
                id = 1,
                dateEpochDay = oct14,
                startMinutes = 600, // 10:00 AM
                endMinutes = 690,   // 11:30 AM
                durationMinutes = 90,
                categoryId = 1,
                batchId = 1,
                categoryNameSnapshot = "Teaching",
                batchNameSnapshot = "Class 10",
                isTeachingSnapshot = true,
                note = "Chapter 5 Trigonometry",
                createdAt = 1000L,
                updatedAt = 1000L
            ),
            WorkSessionEntity(
                id = 2,
                dateEpochDay = oct14,
                startMinutes = 720, // 12:00 PM
                endMinutes = 780,   // 01:00 PM
                durationMinutes = 60,
                categoryId = 2,
                batchId = null,
                categoryNameSnapshot = "Office Work",
                batchNameSnapshot = null,
                isTeachingSnapshot = false,
                note = "Admission inquiries, fees",
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )

        val csv = CsvExporter.generateCsv(sessions, use24Hour = false)

        assertTrue(csv.contains("2026-10-14,10:00 AM,11:30 AM,90,1h 30m,Teaching,Teaching,Class 10,Chapter 5 Trigonometry"))
        // Check escaping of comma in note
        assertTrue(csv.contains("\"Admission inquiries, fees\""))
        // Check summary block
        assertTrue(csv.contains("--- SUMMARY TOTALS ---"))
        assertTrue(csv.contains("Total Sessions,2"))
        assertTrue(csv.contains("Days Worked,1"))
        assertTrue(csv.contains("Total Work Hours,2h 30m"))
        assertTrue(csv.contains("Teaching Hours,1h 30m"))
        assertTrue(csv.contains("Non-Teaching Hours,1h"))
    }

    @Test
    fun generateCsv_specialCharactersEscaped() {
        val oct14 = LocalDate.of(2026, 10, 14).toEpochDay()
        val session = WorkSessionEntity(
            id = 1,
            dateEpochDay = oct14,
            startMinutes = 600,
            endMinutes = 660,
            durationMinutes = 60,
            categoryId = 1,
            batchId = null,
            categoryNameSnapshot = "Test \"Special\", Work",
            batchNameSnapshot = null,
            isTeachingSnapshot = false,
            note = "Line 1\nLine 2",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val csv = CsvExporter.generateCsv(listOf(session), use24Hour = true)
        // Category with quotes and commas
        assertTrue(csv.contains("\"Test \"\"Special\"\", Work\""))
        // Note with newline
        assertTrue(csv.contains("\"Line 1\nLine 2\""))
    }
}
