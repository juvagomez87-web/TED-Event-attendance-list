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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AppUpdateEntity
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TedPink
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UpdatesScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val updates by viewModel.allAppUpdates.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showPostDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (lang == Language.NL) "UPDATES & MELDINGEN" else if (lang == Language.ES) "AVISOS Y NOTIFICACIONES" else "UPDATES & ALERTS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TedPink
                )
                Text(
                    text = AppStrings.get("reminders_title", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showPostDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("post_update_btn")
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Nieuwe Update", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Push Notification & Reminder Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.get("reminders_toggle", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ontvang pushmeldingen vóór raids, briefings en belangrijke aankondigingen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { viewModel.toggleReminders(it) },
                        modifier = Modifier.testTag("toggle_reminders_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { viewModel.triggerTestNotification() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("test_notification_btn")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("test_notification", lang), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "MEDEDELINGEN & VERSIE UPDATES (${updates.size})",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Updates feed
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(updates, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("update_item_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.isPriority) TedPink.copy(alpha = 0.2f) else PrimaryBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.versionOrTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPriority) TedPink else PrimaryBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (item.isPriority) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = TedPink
                                ) {
                                    Text(
                                        text = "PRIORITY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gepubliceerd door ${item.publishedBy} • ${dateFormat.format(Date(item.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showPostDialog) {
        var uTitle by remember { mutableStateOf("") }
        var uTag by remember { mutableStateOf("v2.6.0") }
        var uMsg by remember { mutableStateOf("") }
        var uPriority by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPostDialog = false },
            title = { Text(AppStrings.get("post_update", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uTitle,
                        onValueChange = { uTitle = it },
                        label = { Text("Titel van mededeling / update") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("update_title_field")
                    )
                    OutlinedTextField(
                        value = uTag,
                        onValueChange = { uTag = it },
                        label = { Text("Versie / Tag (bijv. v2.6.0 of BELANGRIJK)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("update_tag_field")
                    )
                    OutlinedTextField(
                        value = uMsg,
                        onValueChange = { uMsg = it },
                        label = { Text("Inhoud bericht") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("update_msg_field")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = uPriority, onCheckedChange = { uPriority = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stuur als hoge prioriteit pushmelding", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uTitle.isNotBlank() && uMsg.isNotBlank()) {
                            viewModel.postAppUpdate(uTitle, uTag, uMsg, uPriority)
                            showPostDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                    modifier = Modifier.testTag("confirm_post_update_btn")
                ) {
                    Text("Plaatsen & Melding Verzenden")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPostDialog = false }) {
                    Text("Annuleren")
                }
            }
        )
    }
}
