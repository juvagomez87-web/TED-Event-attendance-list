package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.repository.MemberAttendanceSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateAndShareReport(
        context: Context,
        events: List<EventEntity>,
        members: List<MemberEntity>,
        summaries: List<MemberAttendanceSummary>,
        periodName: String,
        groupFilter: String,
        lang: Language
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        // Header background banner
        paint.color = Color.rgb(15, 23, 42) // Dark Navy
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("ROSTERTRACK - PARTICIPATION REPORT", 24f, 40f, paint)

        // Subtitle
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText(
            "Urban Bear Discord HQ | Generated on: ${dateFormat.format(Date())}",
            24f,
            62f,
            paint
        )

        // Metadata boxes
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(24f, 105f, 571f, 155f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("FILTER SUMMARY", 36f, 125f, paint)

        paint.isFakeBoldText = false
        val overallRate = if (summaries.isNotEmpty()) {
            summaries.map { it.attendanceRate }.average().toInt()
        } else 0

        canvas.drawText("Period: $periodName   |   Group: $groupFilter   |   Total Members: ${members.size}   |   Events: ${events.size}", 36f, 142f, paint)

        // Metrics highlight
        paint.color = Color.rgb(34, 197, 94) // Green
        paint.textSize = 13f
        paint.isFakeBoldText = true
        canvas.drawText("Average Participation: $overallRate%", 400f, 125f, paint)

        // Table Header
        var y = 180f
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(24f, y - 15f, 571f, y + 10f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("RANK", 32f, y, paint)
        canvas.drawText("MEMBER NAME", 80f, y, paint)
        canvas.drawText("ROLE / GROUP", 220f, y, paint)
        canvas.drawText("ATTENDED", 340f, y, paint)
        canvas.drawText("LATE", 410f, y, paint)
        canvas.drawText("ABSENT", 460f, y, paint)
        canvas.drawText("RATE %", 520f, y, paint)

        y += 24f
        paint.isFakeBoldText = false

        // Member rows
        summaries.take(24).forEachIndexed { index, item ->
            // alternating row color
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(24f, y - 12f, 571f, y + 6f, paint)
            }

            paint.textSize = 8.5f
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText(item.member.rank, 32f, y, paint)

            paint.color = Color.rgb(15, 23, 42)
            paint.isFakeBoldText = true
            canvas.drawText(item.member.name, 80f, y, paint)
            paint.isFakeBoldText = false

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(item.member.groupRole, 220f, y, paint)

            paint.color = Color.rgb(22, 163, 74) // Green
            canvas.drawText("${item.attendedCount}", 355f, y, paint)

            paint.color = Color.rgb(234, 88, 12) // Orange
            canvas.drawText("${item.lateCount}", 420f, y, paint)

            paint.color = Color.rgb(220, 38, 38) // Red
            canvas.drawText("${item.absentCount}", 475f, y, paint)

            val rateColor = when {
                item.attendanceRate >= 75f -> Color.rgb(22, 163, 74)
                item.attendanceRate >= 50f -> Color.rgb(234, 88, 12)
                else -> Color.rgb(220, 38, 38)
            }
            paint.color = rateColor
            paint.isFakeBoldText = true
            canvas.drawText("${item.attendanceRate.toInt()}%", 525f, y, paint)
            paint.isFakeBoldText = false

            y += 20f
        }

        // Recent events recap
        y += 20f
        if (y < 780f) {
            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("RECENT EVENTS EVALUATED", 24f, y, paint)
            y += 16f
            paint.isFakeBoldText = false
            paint.textSize = 8.5f

            events.take(4).forEach { ev ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ev.dateEpochMillis))
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText("• [${ev.category}] ${ev.title} ($dateStr) - Status: ${ev.status}", 32f, y, paint)
                y += 14f
            }
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("RosterTrack System Audit Verified - Real-time cloud & offline synchronized", 24f, 820f, paint)

        pdfDocument.finishPage(page)

        return try {
            val file = File(context.cacheDir, "RosterTrack_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Open share intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "RosterTrack Attendance Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Roster PDF Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
