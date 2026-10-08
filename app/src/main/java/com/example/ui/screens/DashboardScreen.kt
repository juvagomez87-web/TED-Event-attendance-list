package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language

@Composable
fun DashboardScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val members by viewModel.allMembers.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val summaries by viewModel.memberSummaries.collectAsState()
    val selectedPeriod by viewModel.statsPeriod.collectAsState()
    val selectedGroup by viewModel.statsGroup.collectAsState()

    // Filter summaries based on group
    val filteredSummaries = summaries.filter { item ->
        if (selectedGroup == "ALL") true
        else item.member.groupRole.contains(selectedGroup, ignoreCase = true) || item.member.rank.contains(selectedGroup, ignoreCase = true)
    }

    val overallRate = if (filteredSummaries.isNotEmpty()) {
        filteredSummaries.map { it.attendanceRate }.average().toInt()
    } else 0

    val topAttendees = filteredSummaries.take(5)
    val lowAttendees = filteredSummaries.filter { it.attendanceRate < 50f }.take(5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Action Bar: Export PDF & Import PDF
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ADMIN DASHBOARD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TedPink
                )
                Text(
                    text = AppStrings.get("participation_rate", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.exportPdfReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dashboard_export_pdf_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("export_pdf", lang), style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { viewModel.openImportDialog() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dashboard_import_pdf_btn")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("import_pdf", lang), style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { viewModel.openBackupDialog() },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dashboard_backup_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Backup", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Period Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to AppStrings.get("period_all", lang),
                "7D" to AppStrings.get("period_7d", lang),
                "30D" to AppStrings.get("period_30d", lang),
                "MONTH" to AppStrings.get("period_month", lang)
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedPeriod == key,
                    onClick = { viewModel.setStatsPeriod(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_period_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Group / Role Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val groupOptions = listOf("ALL", "Tank", "Healer", "DPS", "Support", "R5/R4", "Officer")
            items(groupOptions) { grp ->
                FilterChip(
                    selected = selectedGroup == grp,
                    onClick = { viewModel.setStatsGroup(grp) },
                    label = { Text(grp, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_group_$grp")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Participation Rate Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = AppStrings.get("overall_attendance", lang),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$overallRate%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    overallRate >= 75 -> StatusPresent
                                    overallRate >= 50 -> StatusLate
                                    else -> StatusAbsent
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Filters: $selectedPeriod • $selectedGroup",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Circular Gauge Ring
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (overallRate / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(80.dp),
                                strokeWidth = 10.dp,
                                color = when {
                                    overallRate >= 75 -> StatusPresent
                                    overallRate >= 50 -> StatusLate
                                    else -> StatusAbsent
                                },
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                            Icon(
                                imageVector = if (overallRate >= 60) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (overallRate >= 60) StatusPresent else StatusAbsent
                            )
                        }
                    }
                }
            }

            // Quick Stats Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = AppStrings.get("total_events", lang),
                        value = "${events.size}",
                        icon = Icons.Default.Event,
                        tint = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = AppStrings.get("total_members", lang),
                        value = "${members.size}",
                        icon = Icons.Default.People,
                        tint = TedPink,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Evaluations",
                        value = "${allAttendance.size}",
                        icon = Icons.Default.Checklist,
                        tint = StatusPresent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Event-by-Event Participation Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EVENT PARTICIPATION RATES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        events.forEach { ev ->
                            val evAttendances = allAttendance.filter { it.eventId == ev.id }
                            val pres = evAttendances.count { it.status == "PRESENT" }
                            val late = evAttendances.count { it.status == "LATE" }
                            val total = maxOf(members.size, 1)
                            val rate = (((pres + (late * 0.75f)) / total) * 100).toInt()

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = ev.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(text = "$rate% ($pres/$total)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { (rate / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = when {
                                        rate >= 75 -> StatusPresent
                                        rate >= 50 -> StatusLate
                                        else -> StatusAbsent
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Top Attendees Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 " + AppStrings.get("top_attendees", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "Raid Priority", style = MaterialTheme.typography.labelSmall, color = StatusPresent)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        topAttendees.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "#${idx + 1}", fontWeight = FontWeight.Bold, color = TedPink, modifier = Modifier.width(26.dp))
                                    Column {
                                        Text(text = item.member.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(text = "${item.member.rank} • ${item.member.groupRole}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StatusPresent.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${item.attendanceRate.toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPresent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Low Attendance Warning Section (<50%)
            if (lowAttendees.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ " + AppStrings.get("low_attendance", lang),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(text = "Needs Review", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            lowAttendees.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = item.member.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(text = "Missed: ${item.absentCount} of ${item.totalEvents} events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StatusAbsent.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${item.attendanceRate.toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusAbsent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
