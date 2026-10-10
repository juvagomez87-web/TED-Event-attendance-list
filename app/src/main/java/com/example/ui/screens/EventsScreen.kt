package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.RosterViewModel
import com.example.util.AppStrings
import com.example.util.Language
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventsScreen(
    viewModel: RosterViewModel,
    modifier: Modifier = Modifier
) {
    val lang by viewModel.currentLanguage.collectAsState()
    val events by viewModel.allEvents.collectAsState()
    val members by viewModel.allMembers.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val selectedEventId by viewModel.selectedEventId.collectAsState()
    val searchQuery by viewModel.memberSearchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showNewEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var activeNoteMember by remember { mutableStateOf<MemberEntity?>(null) }
    var detailedMember by remember { mutableStateOf<MemberEntity?>(null) }
    var showStatusFilterDropdown by remember { mutableStateOf(false) }

    // Kooi 1 and Kooi 2 event lookups
    val kooi1Event = events.find { it.title.contains("kooi 1", ignoreCase = true) || it.title.contains("cage 1", ignoreCase = true) }
    val kooi2Event = events.find { it.title.contains("kooi 2", ignoreCase = true) || it.title.contains("cage 2", ignoreCase = true) }

    // Sorted so Ultieme Kooi 1 and Kooi 2 are ALWAYS shown first
    val sortedEvents = remember(events) {
        events.sortedWith(compareBy<EventEntity> { ev ->
            when {
                ev.title.contains("kooi 1", ignoreCase = true) || ev.title.contains("cage 1", ignoreCase = true) -> 1
                ev.title.contains("kooi 2", ignoreCase = true) || ev.title.contains("cage 2", ignoreCase = true) -> 2
                else -> 3
            }
        }.thenByDescending { it.dateEpochMillis })
    }

    val currentEvent = sortedEvents.find { it.id == selectedEventId } ?: sortedEvents.firstOrNull()
    val eventAttendances = if (currentEvent != null) {
        allAttendance.filter { it.eventId == currentEvent.id }
    } else emptyList()

    val presentCount = eventAttendances.count { it.status == "PRESENT" }
    val otherCageCount = eventAttendances.count { it.status == "OTHER_CAGE" }
    val absentCount = eventAttendances.count { it.status == "ABSENT" }
    val lateCount = eventAttendances.count { it.status == "LATE" }
    val excusedCount = eventAttendances.count { it.status == "EXCUSED" }
    val totalEventMembers = members.size

    val filteredMembers = members.filter { member ->
        val matchesSearch = member.name.contains(searchQuery, ignoreCase = true) ||
                member.handle.contains(searchQuery, ignoreCase = true) ||
                member.groupRole.contains(searchQuery, ignoreCase = true) ||
                member.rank.contains(searchQuery, ignoreCase = true)

        val memberAttendance = eventAttendances.find { it.memberId == member.id }
        val memberStatus = memberAttendance?.status ?: "ABSENT"

        val matchesStatus = when (statusFilter) {
            "ALL" -> true
            else -> memberStatus.equals(statusFilter, ignoreCase = true)
        }

        matchesSearch && matchesStatus
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f))
    ) {
        // Event Tabs / Carousel (Each event has a separate section)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EVENT SECTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.openScreenshotDialog() },
                            modifier = Modifier.size(32.dp).testTag("event_scan_screenshot_icon")
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = "Screenshot Scannen", tint = TedPink, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { viewModel.openImportDialog() },
                            modifier = Modifier.size(32.dp).testTag("event_import_pdf_icon")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Import PDF", tint = TedPink, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { showNewEventDialog = true },
                            modifier = Modifier.size(32.dp).testTag("event_add_icon")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Event", tint = TedPink, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Quick Cage 1 <-> Cage 2 Switcher Banner
                if (kooi1Event != null && kooi2Event != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isK1 = currentEvent?.id == kooi1Event.id
                        val isK2 = currentEvent?.id == kooi2Event.id

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isK1) TedPink else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectEvent(kooi1Event.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Fort,
                                    contentDescription = null,
                                    tint = if (isK1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ultieme Kooi 1",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isK1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isK1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isK2) StatusOtherCage else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.selectEvent(kooi2Event.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Fort,
                                    contentDescription = null,
                                    tint = if (isK2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ultieme Kooi 2",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isK2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isK2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    items(sortedEvents) { ev ->
                        val isSelected = ev.id == currentEvent?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectEvent(ev.id) },
                            label = {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (ev.category.lowercase()) {
                                        "raid" -> Icons.Default.Shield
                                        "war" -> Icons.Default.SportsKabaddi
                                        "meeting" -> Icons.Default.Groups
                                        else -> Icons.Default.Event
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) TedPink else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TedPink.copy(alpha = 0.15f),
                                selectedLabelColor = TedPink
                            ),
                            modifier = Modifier.testTag("event_tab_${ev.id}")
                        )
                    }
                }
            }
        }

        if (currentEvent != null) {
            // Selected Event Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentEvent.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (currentEvent.status) {
                                        "COMPLETED" -> StatusPresent.copy(alpha = 0.2f)
                                        "ACTIVE" -> TedPink.copy(alpha = 0.2f)
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Text(
                                        text = currentEvent.status,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentEvent.status) {
                                            "COMPLETED" -> StatusPresent
                                            "ACTIVE" -> TedPink
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${dateFormat.format(Date(currentEvent.dateEpochMillis))} • ${currentEvent.locationOrChannel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.openQrCheckIn(currentEvent) },
                                modifier = Modifier.testTag("event_qr_checkin_btn")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = "1-Tap QR Check-in", tint = TedPink, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { viewModel.testSendDiscordWebhook() },
                                modifier = Modifier.testTag("event_discord_webhook_btn")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send to Discord", tint = Color(0xFF5865F2), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { editingEvent = currentEvent }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Event", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Colored Attendance Progress Breakdown Bar (matches Screenshot 1)
                    val totalTracked = maxOf(totalEventMembers, 1)
                    val presentFraction = presentCount.toFloat() / totalTracked
                    val otherCageFraction = otherCageCount.toFloat() / totalTracked
                    val lateFraction = lateCount.toFloat() / totalTracked
                    val excusedFraction = excusedCount.toFloat() / totalTracked
                    val absentFraction = (totalTracked - presentCount - otherCageCount - lateCount - excusedCount).toFloat() / totalTracked

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        if (presentFraction > 0f) {
                            Box(modifier = Modifier.weight(maxOf(presentFraction, 0.01f)).fillMaxHeight().background(StatusPresent))
                        }
                        if (otherCageFraction > 0f) {
                            Box(modifier = Modifier.weight(maxOf(otherCageFraction, 0.01f)).fillMaxHeight().background(StatusOtherCage))
                        }
                        if (lateFraction > 0f) {
                            Box(modifier = Modifier.weight(maxOf(lateFraction, 0.01f)).fillMaxHeight().background(StatusLate))
                        }
                        if (excusedFraction > 0f) {
                            Box(modifier = Modifier.weight(maxOf(excusedFraction, 0.01f)).fillMaxHeight().background(StatusExcused))
                        }
                        if (absentFraction > 0f) {
                            Box(modifier = Modifier.weight(maxOf(absentFraction, 0.01f)).fillMaxHeight().background(StatusAbsent))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Breakdown count pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "✔ $presentCount ${AppStrings.get("status_present", lang)}", style = MaterialTheme.typography.labelSmall, color = StatusPresent, fontWeight = FontWeight.Bold)
                        if (otherCageCount > 0 || currentEvent.title.contains("kooi", ignoreCase = true)) {
                            Text(text = "⚡ $otherCageCount ${AppStrings.get("status_other_cage", lang)}", style = MaterialTheme.typography.labelSmall, color = StatusOtherCage, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "⏱ $lateCount ${AppStrings.get("status_late", lang)}", style = MaterialTheme.typography.labelSmall, color = StatusLate, fontWeight = FontWeight.Bold)
                        Text(text = "✖ $absentCount ${AppStrings.get("status_absent", lang)}", style = MaterialTheme.typography.labelSmall, color = StatusAbsent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar & Filter Controls (from Screenshot 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setMemberSearchQuery(it) },
                    placeholder = { Text(AppStrings.get("search_members", lang), style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("search_members_input")
                )

                // Status Filter Dropdown Box
                Box {
                    OutlinedButton(
                        onClick = { showStatusFilterDropdown = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(50.dp).testTag("filter_status_dropdown")
                    ) {
                        Text(
                            text = when (statusFilter) {
                                "PRESENT" -> AppStrings.get("status_present", lang)
                                "ABSENT" -> AppStrings.get("status_absent", lang)
                                "LATE" -> AppStrings.get("status_late", lang)
                                "EXCUSED" -> AppStrings.get("status_excused", lang)
                                else -> AppStrings.get("status_all", lang)
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showStatusFilterDropdown,
                        onDismissRequest = { showStatusFilterDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_all", lang)) },
                            onClick = { viewModel.setStatusFilter("ALL"); showStatusFilterDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_present", lang)) },
                            onClick = { viewModel.setStatusFilter("PRESENT"); showStatusFilterDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_absent", lang)) },
                            onClick = { viewModel.setStatusFilter("ABSENT"); showStatusFilterDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_late", lang)) },
                            onClick = { viewModel.setStatusFilter("LATE"); showStatusFilterDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_other_cage", lang)) },
                            onClick = { viewModel.setStatusFilter("OTHER_CAGE"); showStatusFilterDropdown = false }
                        )
                        DropdownMenuItem(
                            text = { Text(AppStrings.get("status_excused", lang)) },
                            onClick = { viewModel.setStatusFilter("EXCUSED"); showStatusFilterDropdown = false }
                        )
                    }
                }
            }

            // Prominent Hero Action: Scan Screenshots & Koppel aan Event
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { viewModel.openScreenshotDialog() }
                    .testTag("quick_scan_screenshot_hero_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = TedPink.copy(alpha = 0.22f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, TedPink.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(TedPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Screenshots Opladen & Scannen",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TedPink.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = "AI VISION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TedPink,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Koppel uitslagen aan ${currentEvent.title} (of kies ander event)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = TedPink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Secondary Quick Actions: All Present / All Absent / Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.batchSetAttendance(currentEvent.id, "PRESENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusPresent.copy(alpha = 0.15f), contentColor = StatusPresent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("all_present_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("all_present", lang), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.batchSetAttendance(currentEvent.id, "ABSENT") },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsent.copy(alpha = 0.15f), contentColor = StatusAbsent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp).testTag("all_absent_btn")
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("all_absent", lang), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.batchSetAttendance(currentEvent.id, "ABSENT") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(40.dp).testTag("reset_btn")
                ) {
                    Text(AppStrings.get("reset", lang), style = MaterialTheme.typography.labelSmall)
                }
            }

            // Member Attendance Checklist
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredMembers, key = { it.id }) { member ->
                    val attendance = eventAttendances.find { it.memberId == member.id }
                    val currentStatus = attendance?.status ?: "ABSENT"
                    val currentNote = attendance?.notes ?: ""

                    MemberChecklistRow(
                        member = member,
                        status = currentStatus,
                        note = currentNote,
                        onSetStatus = { newStatus ->
                            viewModel.setMemberAttendance(currentEvent.id, member.id, newStatus, currentNote)
                        },
                        onOpenNote = { activeNoteMember = member },
                        onClickMember = { detailedMember = member }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showNewEventDialog) {
        EventEditDialog(
            initialEvent = null,
            lang = lang,
            onDismiss = { showNewEventDialog = false },
            onSave = {
                viewModel.saveEvent(it)
                showNewEventDialog = false
            }
        )
    }

    if (editingEvent != null) {
        EventEditDialog(
            initialEvent = editingEvent,
            lang = lang,
            onDismiss = { editingEvent = null },
            onSave = {
                viewModel.saveEvent(it)
                editingEvent = null
            }
        )
    }

    if (activeNoteMember != null && currentEvent != null) {
        val member = activeNoteMember!!
        val existingNote = eventAttendances.find { it.memberId == member.id }?.notes ?: ""
        val currentStatus = eventAttendances.find { it.memberId == member.id }?.status ?: "ABSENT"

        NoteEditDialog(
            member = member,
            currentNote = existingNote,
            lang = lang,
            onDismiss = { activeNoteMember = null },
            onSave = { noteText ->
                viewModel.setMemberAttendance(currentEvent.id, member.id, currentStatus, noteText)
                activeNoteMember = null
            }
        )
    }

    if (detailedMember != null) {
        MemberDetailDialog(
            member = detailedMember!!,
            events = events,
            attendanceList = allAttendance,
            lang = lang,
            viewModel = viewModel,
            onDismiss = { detailedMember = null }
        )
    }
}

@Composable
fun MemberChecklistRow(
    member: MemberEntity,
    status: String,
    note: String,
    onSetStatus: (String) -> Unit,
    onOpenNote: () -> Unit,
    onClickMember: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_row_${member.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Details
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClickMember() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = member.handle,
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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = member.rank,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = member.groupRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Special participation badge if member is in other cage
                    if (status == "OTHER_CAGE") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StatusOtherCage.copy(alpha = 0.18f),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = StatusOtherCage, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (note.isNotBlank()) note else "Deelgenomen aan andere kooi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusOtherCage,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Right: Interactive Status & Note Action Buttons (matches Screenshots)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkmark (Present)
                IconButton(
                    onClick = { onSetStatus("PRESENT") },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (status == "PRESENT") StatusPresent else StatusPresent.copy(alpha = 0.12f))
                        .testTag("btn_present_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Present",
                        tint = if (status == "PRESENT") Color.White else StatusPresent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Other Cage (Kooi 1 <-> Kooi 2 exchange)
                IconButton(
                    onClick = { onSetStatus("OTHER_CAGE") },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (status == "OTHER_CAGE") StatusOtherCage else StatusOtherCage.copy(alpha = 0.12f))
                        .testTag("btn_other_cage_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Andere Kooi",
                        tint = if (status == "OTHER_CAGE") Color.White else StatusOtherCage,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Cross (Absent)
                IconButton(
                    onClick = { onSetStatus("ABSENT") },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (status == "ABSENT") StatusAbsent else StatusAbsent.copy(alpha = 0.12f))
                        .testTag("btn_absent_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Absent",
                        tint = if (status == "ABSENT") Color.White else StatusAbsent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clock (Late)
                IconButton(
                    onClick = { onSetStatus("LATE") },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (status == "LATE") StatusLate else StatusLate.copy(alpha = 0.12f))
                        .testTag("btn_late_${member.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Late",
                        tint = if (status == "LATE") Color.White else StatusLate,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Note / Chat (Badge if note exists)
                IconButton(
                    onClick = onOpenNote,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (note.isNotBlank()) PrimaryBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_note_${member.id}")
                ) {
                    BadgedBox(
                        badge = {
                            if (note.isNotBlank()) {
                                Badge(containerColor = PrimaryBlue) {
                                    Text("!", fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (note.isNotBlank()) Icons.Default.Chat else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Note",
                            tint = if (note.isNotBlank()) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
