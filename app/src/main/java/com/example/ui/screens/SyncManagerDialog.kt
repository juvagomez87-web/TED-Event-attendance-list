package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.sync.ConflictResolutionPolicy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncManagerDialog(
    viewModel: RosterViewModel,
    onDismiss: () -> Unit
) {
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncTime by viewModel.lastSyncTimestamp.collectAsState()
    val conflictPolicy by viewModel.conflictPolicy.collectAsState()
    val thresholdRate by viewModel.attendanceThreshold.collectAsState()
    val webhookUrl by viewModel.discordWebhookUrl.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val pendingCount by viewModel.pendingSyncCount.collectAsState()
    val conflictAttendances by viewModel.conflictAttendances.collectAsState()
    val members by viewModel.allMembers.collectAsState()

    var tempWebhook by remember { mutableStateOf(webhookUrl) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Sync & Cloud, 1 = Discord, 2 = Drempel Regels
    var jsonExportString by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
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
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = TedPink, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cloud Sync & Integraties",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cross-platform sync, Discord webhooks & raid-drempels",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Sync & Offline", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Discord Webhook", style = MaterialTheme.typography.labelMedium) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Raid Drempel", style = MaterialTheme.typography.labelMedium) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedTab) {
                    0 -> {
                        // Tab 0: Sync & Offline Management
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isOnline) StatusPresent else StatusLate)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isOnline) "🟢 Verbonden met Cloud (Online)" else "🟡 Offline Modus (Lokaal opgeslagen)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "$pendingCount wachtend",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Laatst gesynchroniseerd: ${dateFormat.format(Date(lastSyncTime))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.triggerManualSync() },
                                        enabled = !isSyncing && isOnline,
                                        colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("btn_sync_now")
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nu Synchroniseren")
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.toggleSimulatedOffline() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("btn_toggle_offline_sim")
                                    ) {
                                        Text(if (isOnline) "Simuleer Offline" else "Ga Online")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Conflict Policy Selector
                        Text("Conflict Oplossingsstrategie bij herverbinden:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                ConflictResolutionPolicy.MANUAL_REVIEW to "Handmatig (Aanbevolen)",
                                ConflictResolutionPolicy.LAST_WRITE_WINS to "Laatste Wijziging",
                                ConflictResolutionPolicy.ADMIN_PRIORITY to "Beheerder Prioriteit"
                            ).forEach { (policy, label) ->
                                FilterChip(
                                    selected = conflictPolicy == policy,
                                    onClick = { viewModel.setConflictPolicy(policy) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Conflict Review List
                        if (conflictAttendances.isNotEmpty()) {
                            Text("⚠️ Conflicten die beoordeling vereisen (${conflictAttendances.size}):", style = MaterialTheme.typography.titleSmall, color = StatusAbsent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(conflictAttendances) { att ->
                                    val member = members.find { it.id == att.memberId }
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = "Lid: ${member?.name ?: "#${att.memberId}"}", fontWeight = FontWeight.Bold)
                                            Text(text = "Lokaal: ${att.status} (${att.notes})", style = MaterialTheme.typography.bodySmall, color = StatusPresent)
                                            Text(text = "Server/iOS: ${att.conflictServerStatus} (${att.conflictServerNotes}) door ${att.conflictServerUpdatedBy}", style = MaterialTheme.typography.bodySmall, color = StatusAbsent)

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { viewModel.resolveConflict(att.id, att.status, att.notes, "KEEP_LOCAL") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Behoud Lokaal", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Button(
                                                    onClick = { viewModel.resolveConflict(att.id, att.conflictServerStatus ?: "ABSENT", att.conflictServerNotes ?: "", "KEEP_SERVER") },
                                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Behoud Server", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Cross-Platform Sync Packet Export
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Platformonafhankelijk Synchronisatiepakket (Android & iOS):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Exporteer of importeer een compleet versleuteld JSON rosterpakket voor naadloze synchronisatie met iOS beheerders.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.exportSyncBundle { json -> jsonExportString = json }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Exporteer iOS Bundle", style = MaterialTheme.typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.simulateTestConflict() },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Test Conflict Simulatie", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Tab 1: Discord Webhook Integration
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Automatische Discord Raid-Roster Koppeling",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zodra een evenement wordt afgesloten, stuurt TED PRIVATE automatisch een fraaie embed-samenvatting met Tank/Healer/DPS verdeling naar uw Discord-kanaal (#aankondigingen of #raid-roster).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = tempWebhook,
                                onValueChange = { tempWebhook = it },
                                label = { Text("Discord Webhook URL") },
                                placeholder = { Text("https://discord.com/api/webhooks/...") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth().testTag("input_discord_webhook")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.saveDiscordWebhook(tempWebhook)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("btn_save_webhook")
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Webhook Opslaan")
                                }

                                OutlinedButton(
                                    onClick = { viewModel.testSendDiscordWebhook() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_test_webhook")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Test Bericht")
                                }
                            }
                        }
                    }

                    2 -> {
                        // Tab 2: Automatische Sanctie / Prioriteit Drempel
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Automatische Sanctie & Prioriteit Drempel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Leden met minimaal $thresholdRate% aanwezigheid in de laatste 30 dagen krijgen automatisch het label 'RAID READY'. Leden onder deze drempel worden automatisch gemarkeerd als 'INACTIEF - BESPREKEN'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Minimale Aanwezigheidsdrempel:", fontWeight = FontWeight.SemiBold)
                                        Text(text = "$thresholdRate%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = TedPink)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Slider(
                                        value = thresholdRate.toFloat(),
                                        onValueChange = { viewModel.setAttendanceThreshold(it.toInt()) },
                                        valueRange = 50f..90f,
                                        steps = 7,
                                        modifier = Modifier.fillMaxWidth().testTag("slider_threshold")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("50% (Soepel)", style = MaterialTheme.typography.labelSmall)
                                        Text("70% (Standaard)", style = MaterialTheme.typography.labelSmall)
                                        Text("90% (Strict)", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = StatusPresent.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🛡️ RAID READY", fontWeight = FontWeight.Bold, color = StatusPresent)
                                        Text("≥ $thresholdRate% aanwezigheid. Heeft gegarandeerde plek in main raid lineups.", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = StatusAbsent.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("⚠️ INACTIEF - BESPREKEN", fontWeight = FontWeight.Bold, color = StatusAbsent)
                                        Text("< $thresholdRate% aanwezigheid. Wordt gemarkeerd voor leidinggevend gesprek.", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // Feedback message
                syncMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
