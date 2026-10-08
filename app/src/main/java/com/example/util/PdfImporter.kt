package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.repository.ParsedMemberRecord
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

data class ParsedPdfResult(
    val eventTitle: String,
    val eventCategory: String,
    val eventDateMillis: Long,
    val records: List<ParsedMemberRecord>,
    val rawTextExtracted: String,
    val sourceFileName: String = ""
)

object PdfImporter {

    fun extractTextFromUri(context: Context, uri: Uri): String {
        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // First attempt: read as stream
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                var line = reader.readLine()
                var insideStream = false
                val streamContent = StringBuilder()

                while (line != null) {
                    if (line.contains("stream")) {
                        insideStream = true
                    } else if (line.contains("endstream")) {
                        insideStream = false
                    }

                    // Extract text tokens from PDF standard formats: (Hello World) Tj or [(Hello) 10 (World)] TJ
                    val matcher = Pattern.compile("\\(([^()]+)\\)").matcher(line)
                    while (matcher.find()) {
                        val text = matcher.group(1) ?: ""
                        if (text.isNotBlank() && !text.matches(Regex("^[0-9.]+$"))) {
                            stringBuilder.append(text).append(" ")
                        }
                    }

                    // Also check for plain text lines that look like roster entries
                    if (isLikelyRosterLine(line)) {
                        stringBuilder.append("\n").append(line.trim())
                    }

                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val result = stringBuilder.toString().trim()
        return if (result.length > 20) result else fallbackRawExtract(context, uri)
    }

    private fun fallbackRawExtract(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val ascii = String(bytes.filter { it in 32..126 || it == 10.toByte() || it == 13.toByte() }.toByteArray())
                val lines = ascii.split("\n").filter { isLikelyRosterLine(it) }
                lines.joinToString("\n")
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun isLikelyRosterLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("present") || lower.contains("absent") ||
                lower.contains("aanwezig") || lower.contains("afwezig") ||
                lower.contains("late") || lower.contains("excused") ||
                lower.contains("r5") || lower.contains("r4") || lower.contains("r3")
    }

    fun parseRosterContent(text: String, fallbackTitle: String = "Imported PDF Event"): ParsedPdfResult {
        var eventTitle = fallbackTitle
        var eventCategory = "Raid"
        val now = System.currentTimeMillis()
        val records = mutableListOf<ParsedMemberRecord>()

        val lines = text.split("\n", "\r").map { it.trim() }.filter { it.isNotBlank() }

        // Look for title or header in first few lines
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i]
            if (line.startsWith("Event:", ignoreCase = true) || line.startsWith("Title:", ignoreCase = true)) {
                eventTitle = line.substringAfter(":").trim()
            } else if (line.contains("Raid", ignoreCase = true) || line.contains("War", ignoreCase = true) || line.contains("Meeting", ignoreCase = true)) {
                if (eventTitle == fallbackTitle) {
                    eventTitle = line.take(40).trim()
                }
            }
        }

        // Determine category
        if (eventTitle.contains("War", ignoreCase = true) || eventTitle.contains("Siege", ignoreCase = true)) {
            eventCategory = "War"
        } else if (eventTitle.contains("Meeting", ignoreCase = true) || eventTitle.contains("Briefing", ignoreCase = true)) {
            eventCategory = "Meeting"
        } else if (eventTitle.contains("Dungeon", ignoreCase = true)) {
            eventCategory = "Dungeon"
        }

        // Parse member rows
        lines.forEach { line ->
            // Skip pure header lines
            val lLower = line.lowercase()
            if (lLower == "rank name status note" || lLower.startsWith("member checklist")) return@forEach

            val parsedRecord = parseSingleLine(line)
            if (parsedRecord != null && records.none { it.name.equals(parsedRecord.name, ignoreCase = true) }) {
                records.add(parsedRecord)
            }
        }

        return ParsedPdfResult(
            eventTitle = eventTitle,
            eventCategory = eventCategory,
            eventDateMillis = now,
            records = records,
            rawTextExtracted = text
        )
    }

    private fun parseSingleLine(line: String): ParsedMemberRecord? {
        val lower = line.lowercase()

        // Determine attendance status
        val status = when {
            lower.contains("present") || lower.contains("aanwezig") || lower.contains("presente") -> "PRESENT"
            lower.contains("absent") || lower.contains("afwezig") || lower.contains("ausente") -> "ABSENT"
            lower.contains("late") || lower.contains("te laat") || lower.contains("tarde") -> "LATE"
            lower.contains("excused") || lower.contains("afgemeld") || lower.contains("excusado") -> "EXCUSED"
            else -> "PRESENT" // Default to present if status not explicitly specified
        }

        // Detect Rank (e.g. R5/R4, R5, R4, R3, R2, R1, Officer)
        var rank = "R4"
        val rankRegex = Regex("""\b(R[1-5]/R[1-5]|R[1-5]|Officer|Member|Leader)\b""", RegexOption.IGNORE_CASE)
        val rankMatch = rankRegex.find(line)
        if (rankMatch != null) {
            rank = rankMatch.value.uppercase()
        }

        // Detect Role (Tank, Healer, DPS, Support, Tactician)
        var role = "Member"
        val roleRegex = Regex("""\b(Main Tank|Off Tank|Tank|Healer Lead|Healer|Support|Ranged DPS|Melee DPS|DPS|Tactician|Officer)\b""", RegexOption.IGNORE_CASE)
        val roleMatch = roleRegex.find(line)
        if (roleMatch != null) {
            role = roleMatch.value
        }

        // Detect Handle e.g. @valk
        var handle = ""
        val handleRegex = Regex("""@([a-zA-Z0-9_]+)""")
        val handleMatch = handleRegex.find(line)
        if (handleMatch != null) {
            handle = handleMatch.value
        }

        // Detect note (e.g. "Note: ..." or quoted string or trailing info)
        var note = ""
        if (line.contains("note:", ignoreCase = true)) {
            note = line.substringAfter("note:", "").substringAfter("Note:").trim()
        }

        // Extract member name:
        // Clean tokens removing rank, status, handle, keywords
        var cleaned = line
            .replace(rankRegex, "")
            .replace(roleRegex, "")
            .replace(handleRegex, "")
            .replace(Regex("""(?i)\b(present|absent|late|excused|aanwezig|afwezig|te laat|afgemeld|status|vote|note|rank|name)\b"""), "")
            .replace(Regex("""[|,:;\[\]]"""), " ")
            .trim()

        val tokens = cleaned.split(Regex("""\s+""")).filter { it.length >= 2 }
        val name = tokens.firstOrNull() ?: return null

        if (name.length < 2 || name.matches(Regex("^[0-9]+$"))) return null

        val finalHandle = if (handle.isNotBlank()) handle else "@${name.lowercase().replace(" ", "")}"

        return ParsedMemberRecord(
            name = name,
            handle = finalHandle,
            rank = rank,
            role = role,
            status = status,
            notes = note
        )
    }

    // Sample presets matching user screenshots for instant testing & demonstration
    val SAMPLE_PDF_ROSTERS = listOf(
        SampleRosterPreset(
            title = "TED Clan Raid Import (Exact)",
            sampleText = """
                Event: TED Citadel Raid
                Category: Raid
                Date: 2026-10-08
                
                RANK   NAME                  STATUS   NOTE
                R4/R5  TeddyGirl             Present  Note: Raid leader
                R4/R5  Sagittarius           Present  Note: Strategy lead
                R4/R5  JustineValentina      Present  Note: Defense caller
                R4     THE WOLVERINE         Present
                R4     willow                Present
                R4     Johana O TED O        Late     Note: 10m delay
                R4     Warden NY Falcon      Present
                R4     Disturbed1            Present
                R3     Hans worst            Present
                R3     TEDOMagellan          Present
                R3     Shakey                Present
                R3     minou                 Excused  Note: Work shift
                R3     XxQueenTillyxX        Present
                R3     MadMark               Present
                R3     Gemini                Present
                R3     mathieu               Absent
                R2     The Watcher           Present
                R2     BigHulk               Present
                R2     Spiderman             Present
            """.trimIndent()
        ),
        SampleRosterPreset(
            title = "Alliance Member Checklist",
            sampleText = """
                Event: Fortress Siege Defense
                Category: War
                
                RANK   NAME                  STATUS/VOTE   NOTE
                R4/R5  TeddyGirl             Present       Commander
                R4/R5  Sagittarius           Present       Wall defense
                R4/R5  JustineValentina      Present       Gate defense
                R4     THE WOLVERINE         Present       Flank
                R4     willow                Present       Support
                R3     John Wick79           Present
                R3     Terminator            Present
                R3     Eddypiler             Present
                R2     The Watcher           Present
                R2     NiA                   Present
                R2     Spiderman             Absent
            """.trimIndent()
        ),
        SampleRosterPreset(
            title = "General Guild Assembly",
            sampleText = """
                Event: Monthly Strategy Briefing & Promotions
                Category: Meeting
                
                TeddyGirl, R4/R5, Officer, Present, Raid briefing
                Sagittarius, R4/R5, Officer, Present
                JustineValentina, R4/R5, Officer, Present
                THE WOLVERINE, R4, Core Raid, Present
                willow, R4, Core Raid, Present
                Hans worst, R3, Veteran, Present
                XxQueenTillyxX, R3, Veteran, Present
                ThorBjörn, R3, Veteran, Present
                The Watcher, R2, Member, Present
            """.trimIndent()
        )
    )
}

data class SampleRosterPreset(
    val title: String,
    val sampleText: String
)
