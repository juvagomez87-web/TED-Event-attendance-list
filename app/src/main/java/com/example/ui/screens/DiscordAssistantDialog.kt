package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.EventEntity
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.DiscordShareHelper
import kotlinx.coroutines.launch

@Composable
fun DiscordAssistantDialog(
    viewModel: RosterViewModel,
    selectedEvent: EventEntity?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val discordWebhookUrl by viewModel.discordWebhookUrl.collectAsState()
    var tempWebhook by remember { mutableStateOf(if (discordWebhookUrl.contains("example")) "" else discordWebhookUrl) }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("discord_assistant_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF5865F2) // Discord Blurple
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp).size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Discord #leaders-chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "BLOOD THIRSTY TED ALLIANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Sluiten")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Role / Moderator Notice Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TedPink,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Voor Moderators & Leiders",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "In Discord kan alleen de Server Eigenaar (of iemand met de rol 'Webhooks beheren') een webhook-link maken. Als moderator zie je die optie meestal niet in Discord.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👉 Goed nieuws: je hebt GEEN webhook nodig! Je kunt uitslagen en lijsten direct met 1 klik delen naar Discord of kopiëren.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TedPink
                            )
                        }
                    }

                    // Direct Share & Copy Options (Works for EVERY moderator)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "1. DIRECT DELEN NAAR DISCORD (Aanbevolen)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Maakt een mooi opgemaakt bericht van het huidige evenement (${selectedEvent?.title ?: "Selecteer event"}) en opent Discord:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val event = selectedEvent ?: viewModel.allEvents.value.firstOrNull()
                                        if (event != null) {
                                            val members = viewModel.allMembers.value
                                            val attendance = viewModel.allAttendance.value.filter { it.eventId == event.id }
                                            val present = members.filter { m -> attendance.find { it.memberId == m.id }?.status == "PRESENT" }
                                            
                                            val sb = StringBuilder()
                                            sb.appendLine("🛡️ **[TED PRIVATE] ${event.title} — Aanwezigheidsrapport**")
                                            sb.appendLine("📅 Categorie: ${event.category} | Locatie: ${event.locationOrChannel}")
                                            sb.appendLine("👥 Aanwezig (${present.size}/${members.size}):")
                                            present.take(15).forEach { m -> sb.appendLine("✓ ${m.name} (${m.groupRole})") }
                                            sb.appendLine("\n*Verzonden via TED PRIVATE voor #leaders-chat*")
                                            
                                            DiscordShareHelper.shareToDiscordOrApps(
                                                context = context,
                                                message = sb.toString(),
                                                title = "Deel naar #leaders-chat in Discord"
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open in Discord", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val event = selectedEvent ?: viewModel.allEvents.value.firstOrNull()
                                        if (event != null) {
                                            val members = viewModel.allMembers.value
                                            val attendance = viewModel.allAttendance.value.filter { it.eventId == event.id }
                                            val present = members.filter { m -> attendance.find { it.memberId == m.id }?.status == "PRESENT" }
                                            
                                            val sb = StringBuilder()
                                            sb.appendLine("🛡️ **[TED PRIVATE] ${event.title}**")
                                            sb.appendLine("👥 Aanwezig (${present.size}/${members.size}): " + present.joinToString(", ") { it.name })
                                            
                                            DiscordShareHelper.copyToClipboard(context, sb.toString())
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kopieer Tekst")
                                }
                            }
                        }
                    }

                    // Optional Webhook Configuration (If server owner supplies a webhook)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "2. AUTOMATISCHE DISCORD WEBHOOK (Optioneel)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vraag de Discord server-eigenaar om een Webhook aan te maken voor #leaders-chat (via Serverinstellingen > Integraties > Webhooks). Plak die link hieronder:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = tempWebhook,
                                onValueChange = { tempWebhook = it },
                                label = { Text("Webhook URL (optioneel)") },
                                placeholder = { Text("https://discord.com/api/webhooks/...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_webhook_url")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveDiscordWebhook(tempWebhook)
                                        testStatusMessage = "✓ Webhook opgeslagen!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Opslaan")
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (tempWebhook.isBlank() || !tempWebhook.startsWith("http")) {
                                            testStatusMessage = "Vul eerst een geldige webhook URL in."
                                            return@OutlinedButton
                                        }
                                        isTesting = true
                                        testStatusMessage = "Versturen naar Discord..."
                                        coroutineScope.launch {
                                            val res = com.example.util.DiscordWebhookService.sendTestMessage(tempWebhook, "#leaders-chat")
                                            isTesting = false
                                            testStatusMessage = if (res.isSuccess) "✓ ${res.getOrThrow()}" else "❌ ${res.exceptionOrNull()?.message}"
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isTesting,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test")
                                }
                            }

                            if (testStatusMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = testStatusMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (testStatusMessage!!.startsWith("✓")) Color(0xFF22C55E) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Sluiten", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
