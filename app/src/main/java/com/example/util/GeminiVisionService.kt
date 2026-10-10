package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class LeaderboardScannedPlayer(
    val rank: Int,
    val rawName: String,
    val cleanName: String,
    val score: String,
    var matchedMemberId: Long? = null,
    var matchedMemberName: String? = null,
    var matchedMemberRank: String? = null,
    var isSelected: Boolean = true
)

object PresetLeaderboardSamples {
    // Exact screenshot uploaded by user for Ultieme Kooi 1
    val KOOI_1 = listOf(
        LeaderboardScannedPlayer(1, "[TED]willow", "willow", "4,794,377,933"),
        LeaderboardScannedPlayer(2, "[TED]JustineValentina", "JustineValentina", "2,639,395,401"),
        LeaderboardScannedPlayer(3, "[TED]Hans worst", "Hans worst", "2,542,390,925"),
        LeaderboardScannedPlayer(4, "[TED]Kos", "Kos", "2,488,679,789"),
        LeaderboardScannedPlayer(5, "[TED]Johana \u2B58TED\u2B58", "Johana O TED O", "2,480,810,685"),
        LeaderboardScannedPlayer(6, "[TED]Terminator", "Terminator", "2,133,731,130"),
        LeaderboardScannedPlayer(7, "[TED]KMP", "KMP", "1,574,839,204"),
        LeaderboardScannedPlayer(8, "[TED]Jer", "Jer", "36,985,766"),
        LeaderboardScannedPlayer(9, "[TED]MissQueenJuva", "MissQueenJuva", "2,140,637")
    )

    val SAMPLE_1 = listOf(
        LeaderboardScannedPlayer(1, "[TED]minou", "minou", "1,644,448,049"),
        LeaderboardScannedPlayer(2, "[TED]Spiderman", "Spiderman", "904,154,328"),
        LeaderboardScannedPlayer(3, "[TED]BigHulk", "BigHulk", "858,863,384"),
        LeaderboardScannedPlayer(4, "[TED]Sagittarius", "Sagittarius", "780,289,004"),
        LeaderboardScannedPlayer(5, "[TED]TeddyGirl", "TeddyGirl", "688,337,203"),
        LeaderboardScannedPlayer(6, "[TED]Gemini", "Gemini", "554,516,099"),
        LeaderboardScannedPlayer(7, "[TED]Warden NY Falcon", "Warden NY Falcon", "486,472,801"),
        LeaderboardScannedPlayer(8, "[TED]Disturbed1", "Disturbed1", "433,303,234"),
        LeaderboardScannedPlayer(9, "[TED]WeeWeeJosh", "WeeWeeJosh", "215,568,380")
    )

    val SAMPLE_2 = listOf(
        LeaderboardScannedPlayer(1, "[TED]minou", "minou", "1,644,448,049"),
        LeaderboardScannedPlayer(2, "[TED]Spiderman", "Spiderman", "904,154,328"),
        LeaderboardScannedPlayer(3, "[TED]BigHulk", "BigHulk", "858,863,384"),
        LeaderboardScannedPlayer(4, "[TED]Sagittarius", "Sagittarius", "780,289,004"),
        LeaderboardScannedPlayer(5, "[TED]TeddyGirl", "TeddyGirl", "688,337,203"),
        LeaderboardScannedPlayer(6, "[TED]Gemini", "Gemini", "554,516,099"),
        LeaderboardScannedPlayer(7, "[TED]Warden NY Falcon", "Warden NY Falcon", "486,472,801"),
        LeaderboardScannedPlayer(8, "[TED]Disturbed1", "Disturbed1", "433,303,234"),
        LeaderboardScannedPlayer(9, "[TED]WeeWeeJosh", "WeeWeeJosh", "215,568,380"),
        LeaderboardScannedPlayer(10, "[TED]Libra Rising", "Libra Rising", "146,373,537")
    )
}

object GeminiVisionService {
    private const val TAG = "GeminiVisionService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Cleans clan tags like [TED] or whitespace from a player's raw name.
     */
    fun cleanPlayerName(raw: String): String {
        return raw
            .replace(Regex("\\[TED\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[ted\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^TED\\s*", RegexOption.IGNORE_CASE), "")
            .replace("@", "")
            .trim()
    }

    /**
     * Matches scanned players against existing clan members in the database.
     */
    fun matchWithRoster(
        scanned: List<LeaderboardScannedPlayer>,
        members: List<MemberEntity>
    ): List<LeaderboardScannedPlayer> {
        return scanned.map { player ->
            val clean = cleanPlayerName(player.cleanName.ifBlank { player.rawName })
            val cleanNoSpace = clean.replace(" ", "").lowercase()

            val matched = members.find { m ->
                val mClean = m.name.trim().lowercase()
                val mNoSpace = mClean.replace(" ", "")

                // 1. Exact match
                mClean == clean.lowercase() ||
                // 2. Space-insensitive match
                mNoSpace == cleanNoSpace ||
                // 3. Known Aliases for TED Clan
                (clean.contains("warden", ignoreCase = true) && m.name.contains("warden", ignoreCase = true)) ||
                (clean.equals("TeddyGirl", ignoreCase = true) && m.name.equals("TeddyGirl", ignoreCase = true)) ||
                (clean.equals("BigHulk", ignoreCase = true) && m.name.equals("BigHulk", ignoreCase = true)) ||
                (clean.equals("minou", ignoreCase = true) && m.name.equals("minou", ignoreCase = true)) ||
                (clean.equals("Sagittarius", ignoreCase = true) && m.name.equals("Sagittarius", ignoreCase = true)) ||
                (clean.equals("Libra Rising", ignoreCase = true) && m.name.equals("Libra Rising", ignoreCase = true)) ||
                (clean.equals("WeeWeeJosh", ignoreCase = true) && m.name.equals("WeeWeeJosh", ignoreCase = true)) ||
                (clean.equals("willow", ignoreCase = true) && m.name.equals("willow", ignoreCase = true)) ||
                (clean.equals("JustineValentina", ignoreCase = true) && m.name.equals("JustineValentina", ignoreCase = true)) ||
                (clean.equals("Hans worst", ignoreCase = true) && m.name.equals("Hans worst", ignoreCase = true)) ||
                (clean.equals("Kos", ignoreCase = true) && m.name.equals("Kos", ignoreCase = true)) ||
                (clean.contains("Johana", ignoreCase = true) && m.name.contains("Johana", ignoreCase = true)) ||
                (clean.equals("Terminator", ignoreCase = true) && m.name.equals("Terminator", ignoreCase = true)) ||
                (clean.equals("KMP", ignoreCase = true) && m.name.equals("KMP", ignoreCase = true)) ||
                (clean.equals("Jer", ignoreCase = true) && m.name.equals("Jer", ignoreCase = true)) ||
                ((clean.contains("MissQueenJuva", ignoreCase = true) || clean.contains("MissValxOd", ignoreCase = true)) &&
                        (m.name.contains("MissValxOd", ignoreCase = true) || m.name.contains("MissQueenJuva", ignoreCase = true) || m.name.contains("XxQueenTillyxX", ignoreCase = true))) ||
                // 4. Substring match (length >= 4)
                (clean.length >= 4 && mClean.contains(clean.lowercase())) ||
                (mClean.length >= 4 && clean.lowercase().contains(mClean))
            }

            player.copy(
                cleanName = clean,
                matchedMemberId = matched?.id,
                matchedMemberName = matched?.name,
                matchedMemberRank = matched?.rank
            )
        }
    }

    /**
     * Loads, downsamples, and analyzes a screenshot Uri using Gemini Vision or local OCR.
     */
    suspend fun analyzeScreenshot(
        context: Context,
        imageUri: Uri,
        members: List<MemberEntity>
    ): Result<List<LeaderboardScannedPlayer>> = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadAndDownsampleBitmap(context, imageUri)
                ?: return@withContext Result.failure(Exception("Kan afbeelding niet laden van geselecteerde URI"))

            val scanned = analyzeBitmapWithGemini(bitmap)
            if (scanned.isSuccess) {
                val matched = matchWithRoster(scanned.getOrThrow(), members)
                Result.success(matched)
            } else {
                // If API fails or is unconfigured, return failure with message
                scanned
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fout bij analyseren van screenshot", e)
            Result.failure(e)
        }
    }

    /**
     * Calls Gemini Multimodal REST API to parse leaderboard rankings and doelpunten.
     */
    suspend fun analyzeBitmapWithGemini(
        bitmap: Bitmap
    ): Result<List<LeaderboardScannedPlayer>> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Geen Gemini API key gevonden. Configureer GEMINI_API_KEY of gebruik de ingebouwde test-voorbeelden.")
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                You are an OCR and leaderboard data extractor for the mobile game clan [TED] in Land of Jails.
                Examine this screenshot of clan results / leaderboard with player rankings and scores (labeled "Doelpunten").
                Extract every player entry shown.
                For each entry extract:
                1. rank (integer, e.g. 1, 2, 3...)
                2. name (player username, strip clan tag [TED], e.g. 'minou', 'Spiderman', 'BigHulk', 'Sagittarius')
                3. rawName (the original name as shown e.g. '[TED]minou')
                4. score (the exact points string e.g. '1,644,448,049')
                
                Respond ONLY with a valid JSON array of objects with keys: "rank", "name", "rawName", "score".
                Do not include markdown backticks or explanations.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                })
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(endpoint)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                return@withContext Result.failure(Exception("Gemini API error ($endpoint): $errorBody"))
            }

            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Clean markdown code blocks if any
            val cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            val resultList = mutableListOf<LeaderboardScannedPlayer>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val rank = item.optInt("rank", i + 1)
                val name = item.optString("name", "Speler ${i + 1}")
                val rawName = item.optString("rawName", name)
                val score = item.optString("score", "")

                resultList.add(
                    LeaderboardScannedPlayer(
                        rank = rank,
                        rawName = rawName,
                        cleanName = cleanPlayerName(name),
                        score = score
                    )
                )
            }

            Result.success(resultList)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini vision processing mislukt", e)
            Result.failure(e)
        }
    }

    private fun loadAndDownsampleBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var sampleSize = 1
            val maxDimension = 1200
            while (origWidth / sampleSize > maxDimension || origHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val secondStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(secondStream, null, decodeOptions)
            secondStream?.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Fout bij downsamplen van bitmap", e)
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
