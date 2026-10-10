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
import com.example.data.local.entity.AuditLogEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuditLogScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val logs by viewModel.allAuditLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var actionFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = logs.filter { item ->
        val matchesSearch = item.authorName.contains(searchQuery, ignoreCase = true) ||
                item.entityTarget.contains(searchQuery, ignoreCase = true) ||
                item.details.contains(searchQuery, ignoreCase = true)

        val matchesAction = when (actionFilter) {
            "ALL" -> true
            else -> item.actionType.equals(actionFilter, ignoreCase = true)
        }

        matchesSearch && matchesAction
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
            .padding(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = AppStrings.get("audit_title", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TedPink
            )
            Text(
                text = AppStrings.get("audit_desc", lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Zoek op auteur, lid of event...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("audit_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "ATTENDANCE_CHANGE", "EVENT_CREATE", "BATCH_UPDATE", "PDF_IMPORT").forEach { act ->
                FilterChip(
                    selected = actionFilter == act,
                    onClick = { actionFilter = act },
                    label = {
                        Text(
                            when (act) {
                                "ALL" -> "Alles (${logs.size})"
                                "ATTENDANCE_CHANGE" -> "Aanwezigheid"
                                "EVENT_CREATE" -> "Nieuw Event"
                                "BATCH_UPDATE" -> "Batch Update"
                                "PDF_IMPORT" -> "PDF Import"
                                else -> act
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("audit_chip_$act")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Logs List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("audit_log_item_${log.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = TedPink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.authorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (log.actionType) {
                                    "ATTENDANCE_CHANGE" -> StatusPresent.copy(alpha = 0.15f)
                                    "EVENT_CREATE" -> PrimaryBlue.copy(alpha = 0.15f)
                                    "PDF_IMPORT" -> TedPink.copy(alpha = 0.15f)
                                    "BATCH_UPDATE" -> StatusLate.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = log.actionType.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (log.actionType) {
                                        "ATTENDANCE_CHANGE" -> StatusPresent
                                        "EVENT_CREATE" -> PrimaryBlue
                                        "PDF_IMPORT" -> TedPink
                                        "BATCH_UPDATE" -> StatusLate
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = log.entityTarget,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = log.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
