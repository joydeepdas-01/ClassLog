package com.classlog.app.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.classlog.app.data.local.entity.WorkSessionEntity
import com.classlog.app.domain.calculator.SummaryCalculator
import com.classlog.app.domain.time.TimeFormat
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Generates an offline multi-page PDF attendance and work-hour report.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 36f
    private const val USABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun generateReport(
        yearMonth: YearMonth,
        teacherName: String,
        sessions: List<WorkSessionEntity>,
        use24Hour: Boolean,
        outputStream: OutputStream,
        institutionName: String = ""
    ) {
        val document = PdfDocument()
        val sortedSessions = sessions.sortedWith(
            compareBy<WorkSessionEntity> { it.dateEpochDay }.thenBy { it.startMinutes }
        )
        val summary = SummaryCalculator.summarise(sortedSessions)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(11, 87, 208) // Deep Blue
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(68, 71, 70)
            textSize = 10f
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(31, 31, 31)
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(31, 31, 31)
            textSize = 9f
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.rgb(31, 31, 31)
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(224, 226, 236)
            strokeWidth = 1f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(240, 244, 249)
            isAntiAlias = true
        }

        var currentY = MARGIN

        // --- Helper for Page Break ---
        fun checkPageBreak(requiredHeight: Float) {
            if (currentY + requiredHeight > PAGE_HEIGHT - MARGIN - 40f) {
                // Draw footer on current page
                drawPageFooter(canvas, pageNumber, subtitlePaint)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = MARGIN + 20f

                // Re-draw continuation header
                canvas.drawText("ClassLog Timesheet - Continued (Page $pageNumber)", MARGIN, currentY, subtitlePaint)
                currentY += 15f
                canvas.drawLine(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY, linePaint)
                currentY += 15f
            }
        }

        // --- Document Header ---
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        canvas.drawText("CLASSLOG - MONTHLY TIMESHEET", MARGIN, currentY + 16f, titlePaint)
        currentY += 24f

        val teacherDisplayName = if (teacherName.isNotBlank()) teacherName.trim() else "Teacher"
        val instPart = if (institutionName.isNotBlank()) "  |  Institution: ${institutionName.trim()}" else ""
        val subtitleText = "Teacher: $teacherDisplayName$instPart  |  Period: $monthName ${yearMonth.year}  |  Generated: ${LocalDate.now()}"
        canvas.drawText(subtitleText, MARGIN, currentY + 10f, subtitlePaint)
        currentY += 18f

        canvas.drawLine(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY, linePaint)
        currentY += 14f

        // --- Summary Metric Cards ---
        val cardWidth = USABLE_WIDTH / 4f
        val cardHeight = 44f
        val kpis = listOf(
            "Total Work" to TimeFormat.duration(summary.totalMinutes),
            "Sessions" to "${summary.sessionCount}",
            "Days Worked" to "${summary.daysWorked}",
            "Teaching / Other" to "${TimeFormat.duration(summary.teachingMinutes)} / ${TimeFormat.duration(summary.nonTeachingMinutes)}"
        )

        for (i in kpis.indices) {
            val left = MARGIN + (i * cardWidth)
            val right = left + cardWidth - 6f
            canvas.drawRoundRect(left, currentY, right, currentY + cardHeight, 6f, 6f, cardBgPaint)

            canvas.drawText(kpis[i].first, left + 8f, currentY + 16f, subtitlePaint)
            canvas.drawText(kpis[i].second, left + 8f, currentY + 34f, headerPaint)
        }
        currentY += cardHeight + 20f

        // --- Breakdown by Batch ---
        if (summary.byBatch.isNotEmpty()) {
            checkPageBreak(80f)
            canvas.drawText("Hours by Batch", MARGIN, currentY + 10f, headerPaint)
            currentY += 16f

            val teachingMin = maxOf(summary.teachingMinutes, 1)
            summary.byBatch.entries.sortedByDescending { it.value }.forEach { entry ->
                checkPageBreak(18f)
                val percent = (entry.value * 100f / teachingMin).roundToInt()
                canvas.drawText("• ${entry.key}", MARGIN + 10f, currentY + 10f, bodyPaint)
                canvas.drawText("${TimeFormat.duration(entry.value)}  ($percent% of teaching)", MARGIN + 220f, currentY + 10f, boldBodyPaint)
                currentY += 16f
            }
            currentY += 10f
        }

        // --- Breakdown by Category ---
        if (summary.byCategory.isNotEmpty()) {
            checkPageBreak(80f)
            canvas.drawText("Hours by Category", MARGIN, currentY + 10f, headerPaint)
            currentY += 16f

            val totalMin = maxOf(summary.totalMinutes, 1)
            summary.byCategory.entries.sortedByDescending { it.value }.forEach { entry ->
                checkPageBreak(18f)
                val percent = (entry.value * 100f / totalMin).roundToInt()
                canvas.drawText("• ${entry.key}", MARGIN + 10f, currentY + 10f, bodyPaint)
                canvas.drawText("${TimeFormat.duration(entry.value)}  ($percent%)", MARGIN + 220f, currentY + 10f, boldBodyPaint)
                currentY += 16f
            }
            currentY += 14f
        }

        // --- Detailed Session Log Table ---
        checkPageBreak(60f)
        canvas.drawText("Daily Work Sessions Log", MARGIN, currentY + 10f, headerPaint)
        currentY += 18f

        // Table Column Setup
        // Date (60), Time (75), Category (90), Batch (80), Duration (50), Note (remainder)
        val colDate = MARGIN
        val colTime = colDate + 65f
        val colCat = colTime + 80f
        val colBatch = colCat + 95f
        val colDur = colBatch + 85f
        val colNote = colDur + 55f

        fun drawTableHeader() {
            canvas.drawRect(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY + 18f, cardBgPaint)
            canvas.drawText("Date", colDate + 4f, currentY + 12f, boldBodyPaint)
            canvas.drawText("Time", colTime + 4f, currentY + 12f, boldBodyPaint)
            canvas.drawText("Category", colCat + 4f, currentY + 12f, boldBodyPaint)
            canvas.drawText("Batch", colBatch + 4f, currentY + 12f, boldBodyPaint)
            canvas.drawText("Duration", colDur + 4f, currentY + 12f, boldBodyPaint)
            canvas.drawText("Note", colNote + 4f, currentY + 12f, boldBodyPaint)
            currentY += 20f
        }

        drawTableHeader()

        if (sortedSessions.isEmpty()) {
            canvas.drawText("No sessions recorded in this month.", MARGIN + 10f, currentY + 14f, bodyPaint)
            currentY += 24f
        } else {
            var isAlt = false
            for (session in sortedSessions) {
                checkPageBreak(20f)

                if (isAlt) {
                    canvas.drawRect(MARGIN, currentY - 2f, MARGIN + USABLE_WIDTH, currentY + 16f, cardBgPaint)
                }
                isAlt = !isAlt

                val dateStr = LocalDate.ofEpochDay(session.dateEpochDay).toString()
                val timeStr = "${TimeFormat.clock(session.startMinutes, use24Hour)} - ${TimeFormat.clock(session.endMinutes, use24Hour)}"
                val catStr = session.categoryNameSnapshot
                val batchStr = session.batchNameSnapshot ?: "-"
                val durStr = TimeFormat.duration(session.durationMinutes)
                val noteStr = if (session.note.length > 25) session.note.take(24) + "…" else session.note

                canvas.drawText(dateStr, colDate + 4f, currentY + 10f, bodyPaint)
                canvas.drawText(timeStr, colTime + 4f, currentY + 10f, bodyPaint)
                canvas.drawText(catStr, colCat + 4f, currentY + 10f, bodyPaint)
                canvas.drawText(batchStr, colBatch + 4f, currentY + 10f, bodyPaint)
                canvas.drawText(durStr, colDur + 4f, currentY + 10f, boldBodyPaint)
                canvas.drawText(noteStr, colNote + 4f, currentY + 10f, bodyPaint)

                currentY += 18f
            }
        }

        // --- Signature Section ---
        checkPageBreak(80f)
        currentY += 24f
        canvas.drawLine(MARGIN, currentY, MARGIN + USABLE_WIDTH, currentY, linePaint)
        currentY += 36f

        val sigWidth = 180f
        // Teacher Signature
        canvas.drawLine(MARGIN + 20f, currentY, MARGIN + 20f + sigWidth, currentY, bodyPaint)
        canvas.drawText("Teacher Signature", MARGIN + 20f, currentY + 14f, subtitlePaint)

        // Coordinator Signature
        val coordLeft = MARGIN + USABLE_WIDTH - sigWidth - 20f
        canvas.drawLine(coordLeft, currentY, coordLeft + sigWidth, currentY, bodyPaint)
        canvas.drawText("Centre Coordinator Signature", coordLeft, currentY + 14f, subtitlePaint)

        // Draw last page footer
        drawPageFooter(canvas, pageNumber, subtitlePaint)
        document.finishPage(page)

        // Write to output stream
        document.writeTo(outputStream)
        document.close()
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, paint: Paint) {
        val footerY = PAGE_HEIGHT - MARGIN + 10f
        canvas.drawText("ClassLog • Offline Teacher Attendance & Work Log", MARGIN, footerY, paint)
        canvas.drawText("Page $pageNumber", PAGE_WIDTH - MARGIN - 40f, footerY, paint)
    }
}
