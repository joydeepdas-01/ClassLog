package com.classlog.app.data.export

import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.domain.calculator.SummaryCalculator
import com.classlog.app.domain.time.TimeFormat
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Generates RFC-4180 compliant CSV export for work sessions.
 */
object CsvExporter {

    private val HEADERS = listOf(
        "Date",
        "Start Time",
        "End Time",
        "Duration (Minutes)",
        "Duration",
        "Work Category",
        "Type",
        "Batch",
        "Note"
    )

    fun generateCsv(sessions: List<WorkSessionEntity>, use24Hour: Boolean = false): String {
        val sortedSessions = sessions.sortedWith(
            compareBy<WorkSessionEntity> { it.dateEpochDay }.thenBy { it.startMinutes }
        )
        val sb = StringBuilder()

        // 1. Column Headers
        sb.append(HEADERS.joinToString(",")).append("\r\n")

        // 2. Data Rows
        for (session in sortedSessions) {
            val date = LocalDate.ofEpochDay(session.dateEpochDay).toString()
            val startTime = TimeFormat.clock(session.startMinutes, use24Hour)
            val endTime = TimeFormat.clock(session.endMinutes, use24Hour)
            val durationMinutes = session.durationMinutes.toString()
            val formattedDuration = TimeFormat.duration(session.durationMinutes)
            val category = session.categoryNameSnapshot
            val type = if (session.isTeachingSnapshot) "Teaching" else "Non-Teaching"
            val batch = session.batchNameSnapshot.orEmpty()
            val note = session.note

            val row = listOf(
                escape(date),
                escape(startTime),
                escape(endTime),
                escape(durationMinutes),
                escape(formattedDuration),
                escape(category),
                escape(type),
                escape(batch),
                escape(note)
            )
            sb.append(row.joinToString(",")).append("\r\n")
        }

        // 3. Summary totals section
        if (sortedSessions.isNotEmpty()) {
            val summary = SummaryCalculator.summarise(sortedSessions)
            sb.append("\r\n")
            sb.append("--- SUMMARY TOTALS ---\r\n")
            sb.append("Total Sessions,").append(summary.sessionCount).append("\r\n")
            sb.append("Days Worked,").append(summary.daysWorked).append("\r\n")
            sb.append("Total Work Hours,").append(escape(TimeFormat.duration(summary.totalMinutes))).append("\r\n")
            sb.append("Teaching Hours,").append(escape(TimeFormat.duration(summary.teachingMinutes))).append("\r\n")
            sb.append("Non-Teaching Hours,").append(escape(TimeFormat.duration(summary.nonTeachingMinutes))).append("\r\n")
        }

        return sb.toString()
    }

    fun writeToStream(sessions: List<WorkSessionEntity>, use24Hour: Boolean, outputStream: OutputStream) {
        val csvString = generateCsv(sessions, use24Hour)
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.write(csvString)
            writer.flush()
        }
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
