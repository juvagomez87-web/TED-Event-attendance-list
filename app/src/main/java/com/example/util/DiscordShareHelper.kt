package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiscordShareHelper {

    fun formatLeaderboardForDiscord(
        eventTitle: String,
        players: List<LeaderboardScannedPlayer>
    ): String {
        val selected = players.filter { it.isSelected }
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.appendLine("🏆 **[TED PRIVATE] $eventTitle — Uitslag**")
        sb.appendLine("📅 *Geregistreerd op $dateString*")
        sb.appendLine()
        sb.appendLine("⚔️ **Leaderboard Doelpunten & Deelnemers:**")

        if (selected.isEmpty()) {
            sb.appendLine("*(Geen geregistreerde spelers)*")
        } else {
            selected.forEach { p ->
                val medal = when (p.rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "🔹 #${p.rank}"
                }
                val rankInfo = if (!p.matchedMemberRank.isNullOrBlank()) " (${p.matchedMemberRank})" else ""
                sb.appendLine("$medal **${p.cleanName}**$rankInfo — `${p.score} doelpunten`")
            }
        }

        val matchedCount = selected.count { it.matchedMemberId != null }
        sb.appendLine()
        sb.appendLine("📋 **Clan Status:** $matchedCount/${selected.size} spelers geverifieerd in de TED ledenlijst.")
        sb.appendLine("🛡️ *BLOOD THIRSTY TED ALLIANCE • TED PRIVATE Systeem*")

        return sb.toString()
    }

    fun shareToDiscordOrApps(
        context: Context,
        message: String,
        title: String = "Deel uitslag naar Discord (#leaders-chat)"
    ) {
        try {
            // Also copy to clipboard for convenience
            copyToClipboard(context, message, showToast = false)

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Fout bij delen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "TED Uitslag",
        showToast: Boolean = true
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "✓ Gekopieerd naar klembord! Plak het in #leaders-chat.", Toast.LENGTH_SHORT).show()
        }
    }
}
