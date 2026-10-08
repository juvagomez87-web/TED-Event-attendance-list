package com.example.util

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiscordWebhookService {
    private val client = OkHttpClient()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun sendRosterSummary(
        webhookUrl: String,
        event: EventEntity,
        members: List<MemberEntity>,
        attendanceList: List<AttendanceEntity>,
        thresholdRate: Int = 70
    ): Result<String> = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            return@withContext Result.failure(Exception("Ongeldige of lege Discord Webhook URL"))
        }

        val eventAttendances = attendanceList.filter { it.eventId == event.id }
        val presentMembers = mutableListOf<String>()
        val lateMembers = mutableListOf<String>()
        val absentMembers = mutableListOf<String>()
        val tanks = mutableListOf<String>()
        val healers = mutableListOf<String>()
        val dps = mutableListOf<String>()

        members.forEach { m ->
            val att = eventAttendances.find { it.memberId == m.id }
            val status = att?.status ?: "ABSENT"
            val noteSuffix = if (!att?.notes.isNullOrBlank()) " (${att?.notes})" else ""
            val entry = "${m.name}$noteSuffix"

            when (status) {
                "PRESENT" -> {
                    presentMembers.add(entry)
                    if (m.groupRole.contains("Tank", ignoreCase = true)) tanks.add(m.name)
                    else if (m.groupRole.contains("Healer", ignoreCase = true)) healers.add(m.name)
                    else dps.add(m.name)
                }
                "LATE" -> lateMembers.add(entry)
                else -> absentMembers.add(entry)
            }
        }

        val total = maxOf(members.size, 1)
        val rate = (((presentMembers.size + (lateMembers.size * 0.75f)) / total) * 100).toInt()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        val rootJson = JSONObject().apply {
            put("username", "TED PRIVATE Bot")
            put("avatar_url", "https://raw.githubusercontent.com/aistudio/assets/main/ted_icon.png")
            put("content", "📢 **Aanwezigheidsrapport afgesloten voor event: ${event.title}**")

            val embed = JSONObject().apply {
                put("title", "🛡️ [TED PRIVATE] ${event.title}")
                put("description", "**Categorie:** ${event.category} | **Locatie:** ${event.locationOrChannel}\n**Datum:** ${dateFormat.format(Date(event.dateEpochMillis))}")
                put("color", 0xF43F5E) // TED Pink

                val fields = JSONArray().apply {
                    // Overall Rate
                    put(JSONObject().apply {
                        put("name", "📊 Participatiegraad")
                        put("value", "**$rate%** (${presentMembers.size}/$total aanwezig) • Drempel: $thresholdRate%")
                        put("inline", false)
                    })

                    // Lineup fields
                    if (tanks.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("name", "🛡️ Tanks (${tanks.size})")
                            put("value", tanks.joinToString(", "))
                            put("inline", true)
                        })
                    }
                    if (healers.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("name", "✨ Healers (${healers.size})")
                            put("value", healers.joinToString(", "))
                            put("inline", true)
                        })
                    }
                    if (dps.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("name", "⚔️ DPS / Support (${dps.size})")
                            put("value", dps.take(12).joinToString(", "))
                            put("inline", true)
                        })
                    }

                    // Late or Absent
                    if (lateMembers.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("name", "⏱️ Te Laat (${lateMembers.size})")
                            put("value", lateMembers.joinToString("\n"))
                            put("inline", false)
                        })
                    }
                    if (absentMembers.isNotEmpty()) {
                        put(JSONObject().apply {
                            put("name", "✖️ Afwezig (${absentMembers.size})")
                            put("value", absentMembers.take(10).joinToString(", "))
                            put("inline", false)
                        })
                    }
                }
                put("fields", fields)

                val footer = JSONObject().apply {
                    put("text", "TED PRIVATE Attendance System • Offline-ready & Cloud Synchronized")
                }
                put("footer", footer)
            }

            put("embeds", JSONArray().put(embed))
        }

        try {
            val body = rootJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code in 200..204) {
                    Result.success("Succesvol verstuurd naar Discord!")
                } else {
                    Result.failure(Exception("Discord foutmelding HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
