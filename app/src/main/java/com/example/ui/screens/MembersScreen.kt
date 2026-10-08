package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.local.entity.MemberEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language

@Composable
fun MembersScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val members by viewModel.allMembers.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val summaries by viewModel.memberSummaries.collectAsState()
    val thresholdRate by viewModel.attendanceThreshold.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var detailedMember by remember { mutableStateOf<MemberEntity?>(null) }
    var editingMember by remember { mutableStateOf<MemberEntity?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    val filteredSummaries = summaries.filter { summary ->
        val matchesSearch = summary.member.name.contains(searchQuery, ignoreCase = true) ||
                summary.member.handle.contains(searchQuery, ignoreCase = true) ||
                summary.member.groupRole.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "ALL" -> true
            "RAID_READY" -> summary.attendanceRate >= thresholdRate
            "INACTIVE" -> summary.attendanceRate < thresholdRate
            else -> summary.member.rank.contains(selectedFilter, ignoreCase = true)
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header & Add Member
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TED CLAN ROSTER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TedPink
                )
                Text(
                    text = "${members.size} ${AppStrings.get("total_members", lang)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddMemberDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_member_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("add_member", lang), style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(AppStrings.get("search_members", lang)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("roster_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips (Rank & Automatic Threshold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "ALL" to "Alles (${members.size})",
                "RAID_READY" to "🛡️ Raid Ready",
                "INACTIVE" to "⚠️ Inactief",
                "R4/R5" to "R4/R5",
                "R4" to "R4",
                "R3" to "R3",
                "R2" to "R2"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("filter_chip_$key")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Member List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredSummaries, key = { it.member.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailedMember = item.member }
                        .testTag("roster_item_${item.member.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.member.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.member.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.member.handle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = item.member.rank,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.member.groupRole,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Right: Participation Rate Pill & Threshold Badge
                        Column(horizontalAlignment = Alignment.End) {
                            val isRaidReady = item.attendanceRate >= thresholdRate
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isRaidReady) StatusPresent.copy(alpha = 0.2f) else StatusAbsent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isRaidReady) "🛡️ RAID READY" else "⚠️ INACTIEF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRaidReady) StatusPresent else StatusAbsent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    item.attendanceRate >= thresholdRate -> StatusPresent.copy(alpha = 0.15f)
                                    item.attendanceRate >= 50f -> StatusLate.copy(alpha = 0.15f)
                                    else -> StatusAbsent.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = "${item.attendanceRate.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        item.attendanceRate >= thresholdRate -> StatusPresent
                                        item.attendanceRate >= 50f -> StatusLate
                                        else -> StatusAbsent
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.attendedCount} att / ${item.totalEvents} tot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (detailedMember != null) {
        MemberDetailDialog(
            member = detailedMember!!,
            events = events,
            attendanceList = allAttendance,
            lang = lang,
            onDismiss = { detailedMember = null }
        )
    }

    if (showAddMemberDialog) {
        MemberEditDialog(
            initialMember = null,
            lang = lang,
            onDismiss = { showAddMemberDialog = false },
            onSave = {
                viewModel.saveMember(it)
                showAddMemberDialog = false
            }
        )
    }
}

@Composable
fun MemberEditDialog(
    initialMember: MemberEntity?,
    lang: Language,
    onDismiss: () -> Unit,
    onSave: (MemberEntity) -> Unit
) {
    var name by remember { mutableStateOf(initialMember?.name ?: "") }
    var handle by remember { mutableStateOf(initialMember?.handle ?: "") }
    var rank by remember { mutableStateOf(initialMember?.rank ?: "R4") }
    var role by remember { mutableStateOf(initialMember?.groupRole ?: "Member") }
    var contact by remember { mutableStateOf(initialMember?.contact ?: "") }

    val ranks = listOf("R5/R4", "R4", "R3", "R2", "R1")
    val roles = listOf("Main Tank", "Healer Lead", "Ranged DPS", "Melee DPS", "Support", "Tactician", "Officer")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMember == null) AppStrings.get("add_member", lang) else "Edit Member",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_member_name")
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Discord Handle (@handle)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Rank:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ranks.take(4).forEach { r ->
                        FilterChip(
                            selected = rank == r,
                            onClick = { rank = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Role:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    roles.take(3).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val m = (initialMember ?: MemberEntity(name = name, handle = handle)).copy(
                            name = name.trim(),
                            handle = if (handle.isNotBlank()) handle.trim() else "@${name.lowercase().replace(" ", "")}",
                            rank = rank,
                            groupRole = role,
                            contact = contact.trim()
                        )
                        onSave(m)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TedPink),
                modifier = Modifier.testTag("save_member_btn")
            ) {
                Text(AppStrings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
