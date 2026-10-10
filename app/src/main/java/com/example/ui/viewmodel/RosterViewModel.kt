package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppUpdateEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.remote.FirestoreRosterService
import com.example.data.repository.MemberAttendanceSummary
import com.example.data.repository.RosterRepository
import com.example.data.sync.ConflictResolutionPolicy
import com.example.data.sync.SyncManager
import com.example.util.BackupManager
import com.example.util.DiscordWebhookService
import com.example.util.Language
import com.example.util.NotificationHelper
import com.example.util.ParsedPdfResult
import com.example.util.PdfExporter
import com.example.util.PdfImporter
import com.example.util.LeaderboardScannedPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class RosterViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        fun rankWeight(rank: String): Int {
            val clean = rank.trim().uppercase()
            return when {
                clean == "R4/R5" -> 1
                clean == "R5" -> 2
                clean == "R4" -> 3
                clean == "R3" -> 4
                clean == "R2" -> 5
                clean == "R1" -> 6
                else -> 7
            }
        }

        val ADMIN_JUSTINE = UserAccountEntity(
            id = 1,
            username = "JustineValentina",
            displayName = "JustineValentina",
            emailOrDiscord = "justine@discord.gg",
            pinOrPassword = "",
            role = "ADMIN",
            memberId = 3,
            lastLoginAt = System.currentTimeMillis(),
            loginCount = 1,
            isActive = true
        )
    }

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = RosterRepository(db)
    private val syncManager = SyncManager(application, db, viewModelScope)
    private val firestoreService = FirestoreRosterService()

    init {
        ensureCageEventsExist()
    }

    private fun ensureCageEventsExist() {
        viewModelScope.launch {
            val eventsList = db.eventDao().getAllEventsList()
            var k1 = eventsList.find {
                it.title.contains("kooi 1", ignoreCase = true) ||
                it.title.contains("cage 1", ignoreCase = true) ||
                it.title.equals("Ultimate Cage", ignoreCase = true)
            }
            if (k1 != null && k1.title.equals("Ultimate Cage", ignoreCase = true)) {
                k1 = k1.copy(title = "Ultieme Kooi 1", category = "Ultieme Kooi 1")
                db.eventDao().update(k1)
            } else if (k1 == null) {
                val newK1 = EventEntity(
                    title = "Ultieme Kooi 1",
                    category = "Ultieme Kooi 1",
                    description = "Land of Jails: Ultieme Kooi 1 arena combat & clan verdediging.",
                    dateEpochMillis = System.currentTimeMillis(),
                    targetGroup = "All",
                    status = "ACTIVE",
                    locationOrChannel = "Discord Voice Kooi 1",
                    createdBy = "JustineValentina"
                )
                db.eventDao().insert(newK1)
            }

            val k2 = eventsList.find {
                it.title.contains("kooi 2", ignoreCase = true) ||
                it.title.contains("cage 2", ignoreCase = true)
            }
            if (k2 == null) {
                val newK2 = EventEntity(
                    title = "Ultieme Kooi 2",
                    category = "Ultieme Kooi 2",
                    description = "Land of Jails: Ultieme Kooi 2 arena combat & clan verdediging.",
                    dateEpochMillis = System.currentTimeMillis(),
                    targetGroup = "All",
                    status = "ACTIVE",
                    locationOrChannel = "Discord Voice Kooi 2",
                    createdBy = "JustineValentina"
                )
                db.eventDao().insert(newK2)
            }
        }
    }

    fun findKooi1Event(): EventEntity? {
        return allEvents.value.find {
            it.title.contains("kooi 1", ignoreCase = true) ||
            it.title.contains("cage 1", ignoreCase = true) ||
            it.title.equals("Ultimate Cage", ignoreCase = true)
        }
    }

    fun findKooi2Event(): EventEntity? {
        return allEvents.value.find {
            it.title.contains("kooi 2", ignoreCase = true) ||
            it.title.contains("cage 2", ignoreCase = true)
        }
    }

    fun isKooi1(eventId: Long): Boolean {
        val k1 = findKooi1Event()
        return k1?.id == eventId
    }

    fun isKooi2(eventId: Long): Boolean {
        val k2 = findKooi2Event()
        return k2?.id == eventId
    }

    // Available ranks in Land of Jails (R1 to R5)
    val AVAILABLE_RANKS = listOf("R4/R5", "R5", "R4", "R3", "R2", "R1")

    /**
     * Checks if the current user has permission to edit data / change ranks (minimaal R4 vereist).
     */
    fun canCurrentUserEdit(): Boolean {
        val user = _currentUser.value ?: return false
        if (user.role.equals("ADMIN", ignoreCase = true) || user.role.equals("OFFICER", ignoreCase = true)) {
            return true
        }
        val member = allMembers.value.find { it.name.equals(user.displayName, ignoreCase = true) }
        val memberRank = member?.rank ?: user.role
        return isRankAtLeastR4(memberRank)
    }

    fun isRankAtLeastR4(rank: String): Boolean {
        val r = rank.trim().uppercase()
        return r == "R5" || r == "R4/R5" || r == "R4" || r == "ADMIN"
    }

    // Current active admin user (JustineValentina by default, no password/login required)
    private val _currentUser = MutableStateFlow<UserAccountEntity?>(ADMIN_JUSTINE)
    val currentUser: StateFlow<UserAccountEntity?> = _currentUser.asStateFlow()

    // Language setting
    private val _currentLanguage = MutableStateFlow(Language.NL)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // Active navigation tab
    private val _currentTab = MutableStateFlow("EVENTS")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Selected event for checklist view
    private val _selectedEventId = MutableStateFlow<Long?>(null)
    val selectedEventId: StateFlow<Long?> = _selectedEventId.asStateFlow()

    // Event Checklist filters
    private val _memberSearchQuery = MutableStateFlow("")
    val memberSearchQuery: StateFlow<String> = _memberSearchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    // Dashboard filters
    private val _statsPeriod = MutableStateFlow("ALL")
    val statsPeriod: StateFlow<String> = _statsPeriod.asStateFlow()

    private val _statsGroup = MutableStateFlow("ALL")
    val statsGroup: StateFlow<String> = _statsGroup.asStateFlow()

    // PDF Import state
    private val _parsedPdfResult = MutableStateFlow<ParsedPdfResult?>(null)
    val parsedPdfResult: StateFlow<ParsedPdfResult?> = _parsedPdfResult.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    // Sync Dialog
    private val _showSyncDialog = MutableStateFlow(false)
    val showSyncDialog: StateFlow<Boolean> = _showSyncDialog.asStateFlow()

    // Encrypted Backup Dialog
    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    fun openBackupDialog() { _showBackupDialog.value = true }
    fun closeBackupDialog() { _showBackupDialog.value = false }

    // Screenshot Scanner Dialog
    private val _showScreenshotDialog = MutableStateFlow(false)
    val showScreenshotDialog: StateFlow<Boolean> = _showScreenshotDialog.asStateFlow()

    fun openScreenshotDialog() { _showScreenshotDialog.value = true }
    fun closeScreenshotDialog() { _showScreenshotDialog.value = false }

    // Discord Assistant Dialog
    private val _showDiscordDialog = MutableStateFlow(false)
    val showDiscordDialog: StateFlow<Boolean> = _showDiscordDialog.asStateFlow()

    fun openDiscordDialog() { _showDiscordDialog.value = true }
    fun closeDiscordDialog() { _showDiscordDialog.value = false }

    // QR Check-in Dialog
    private val _qrCheckInEvent = MutableStateFlow<EventEntity?>(null)
    val qrCheckInEvent: StateFlow<EventEntity?> = _qrCheckInEvent.asStateFlow()

    // Notification / Reminder settings
    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    // UI feedback message
    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    // Automated Threshold & Webhook settings
    private val _attendanceThreshold = MutableStateFlow(70) // Default 70%
    val attendanceThreshold: StateFlow<Int> = _attendanceThreshold.asStateFlow()

    private val _discordWebhookUrl = MutableStateFlow("https://discord.com/api/webhooks/example/urban-bear-raid-roster")
    val discordWebhookUrl: StateFlow<String> = _discordWebhookUrl.asStateFlow()

    // Sync Manager flows
    val isOnline: StateFlow<Boolean> = syncManager.isOnline
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val lastSyncTimestamp: StateFlow<Long> = syncManager.lastSyncTimestamp
    val conflictPolicy: StateFlow<ConflictResolutionPolicy> = syncManager.conflictPolicy
    val syncMessage: StateFlow<String?> = syncManager.syncMessage

    val pendingSyncCount: StateFlow<Int> = db.syncQueueDao().getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val conflictAttendances: StateFlow<List<AttendanceEntity>> = db.attendanceDao().getConflictAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Repository flows
    val allMembers = repository.allMembers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allEvents = repository.allEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAttendance = repository.allAttendance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAppUpdates = repository.allAppUpdates.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allUserAccounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined attendance statistics per member with Threshold calculation
    val memberSummaries: StateFlow<List<MemberAttendanceSummary>> = combine(
        allMembers,
        allEvents,
        allAttendance
    ) { members, events, attendanceList ->
        val completedEventIds = events.map { it.id }.toSet()
        members.map { member ->
            val memberAttendances = attendanceList.filter { it.memberId == member.id && completedEventIds.contains(it.eventId) }
            val attended = memberAttendances.count { it.status == "PRESENT" || it.status == "OTHER_CAGE" }
            val late = memberAttendances.count { it.status == "LATE" }
            val excused = memberAttendances.count { it.status == "EXCUSED" }
            val absent = memberAttendances.count { it.status == "ABSENT" }
            val effectiveAttended = attended + (late * 0.75f)
            val total = memberAttendances.size
            val rate = if (total > 0) (effectiveAttended / total) * 100f else 0f

            MemberAttendanceSummary(
                member = member,
                totalEvents = total,
                attendedCount = attended,
                lateCount = late,
                excusedCount = excused,
                absentCount = absent,
                attendanceRate = rate
            )
        }.sortedWith(
            compareBy<MemberAttendanceSummary> { rankWeight(it.member.rank) }
                .thenBy { it.member.id }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allEvents.collect { events ->
                if (_selectedEventId.value == null && events.isNotEmpty()) {
                    _selectedEventId.value = events.first().id
                }
            }
        }
    }

    fun setLanguage(lang: Language) { _currentLanguage.value = lang }
    fun setTab(tab: String) { _currentTab.value = tab }
    fun selectEvent(eventId: Long) { _selectedEventId.value = eventId }
    fun setMemberSearchQuery(query: String) { _memberSearchQuery.value = query }
    fun setStatusFilter(filter: String) { _statusFilter.value = filter }
    fun setStatsPeriod(period: String) { _statsPeriod.value = period }
    fun setStatsGroup(group: String) { _statsGroup.value = group }
    fun clearSnackBar() { _snackBarMessage.value = null }

    fun openSyncDialog() { _showSyncDialog.value = true }
    fun closeSyncDialog() { _showSyncDialog.value = false }

    fun openQrCheckIn(event: EventEntity) { _qrCheckInEvent.value = event }
    fun closeQrCheckIn() { _qrCheckInEvent.value = null }

    fun setAttendanceThreshold(threshold: Int) {
        _attendanceThreshold.value = threshold
        _snackBarMessage.value = "Raid-drempel ingesteld op $threshold%"
    }

    fun saveDiscordWebhook(url: String) {
        _discordWebhookUrl.value = url.trim()
        _snackBarMessage.value = "Discord Webhook opgeslagen."
    }

    fun testSendDiscordWebhook() {
        val event = allEvents.value.find { it.id == _selectedEventId.value } ?: allEvents.value.firstOrNull() ?: return
        viewModelScope.launch {
            val result = DiscordWebhookService.sendRosterSummary(
                webhookUrl = _discordWebhookUrl.value,
                event = event,
                members = allMembers.value,
                attendanceList = allAttendance.value,
                thresholdRate = _attendanceThreshold.value
            )
            if (result.isSuccess) {
                _snackBarMessage.value = "Discord webhook succesvol verzonden!"
            } else {
                _snackBarMessage.value = "Fout bij verzenden: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    // Sync operations
    fun triggerManualSync() {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            syncManager.syncPendingChanges(author)
        }
    }

    fun toggleSimulatedOffline() {
        val current = syncManager.isOnline.value
        syncManager.setSimulatedOffline(current)
    }

    fun setConflictPolicy(policy: ConflictResolutionPolicy) {
        syncManager.setConflictPolicy(policy)
    }

    fun resolveConflict(attendanceId: Long, chosenStatus: String, chosenNotes: String, resolutionType: String) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            syncManager.resolveConflictManually(attendanceId, chosenStatus, chosenNotes, author, resolutionType)
        }
    }

    fun simulateTestConflict() {
        val currentEvent = allEvents.value.firstOrNull() ?: return
        val currentMember = allMembers.value.firstOrNull() ?: return
        viewModelScope.launch {
            syncManager.simulateServerConflict(currentEvent.id, currentMember.id)
        }
    }

    fun exportSyncBundle(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = syncManager.exportCrossPlatformSyncBundle()
            onResult(json)
            _snackBarMessage.value = "iOS/Android synchronisatiebundel gegenereerd!"
        }
    }

    fun toggleReminders(enabled: Boolean) {
        _remindersEnabled.value = enabled
        if (enabled) {
            NotificationHelper.showNotification(
                getApplication(),
                "TED PRIVATE",
                "Event reminders activated. You will receive notifications before clan raids and briefings."
            )
        }
    }

    // User Authentication
    fun login(username: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val account = repository.login(username, pin)
            if (account != null) {
                _currentUser.value = account
                onResult(true, "Welcome back, ${account.displayName}!")
            } else {
                onResult(false, "Invalid username or PIN/password.")
            }
        }
    }

    fun register(
        username: String,
        displayName: String,
        emailOrDiscord: String,
        pin: String,
        role: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.register(username, displayName, emailOrDiscord, pin, role)
            if (result.isSuccess) {
                _currentUser.value = result.getOrNull()
                onResult(true, "Account created successfully!")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to create account")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    // Attendance operations with offline queueing and Cage 1 <-> Cage 2 cross-sync
    fun setMemberAttendance(
        eventId: Long,
        memberId: Long,
        status: String,
        notes: String
    ) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            val existing = db.attendanceDao().getSpecific(eventId, memberId)
            val updated = existing?.copy(
                status = status,
                notes = notes,
                updatedBy = author,
                updatedAt = System.currentTimeMillis()
            ) ?: AttendanceEntity(
                eventId = eventId,
                memberId = memberId,
                status = status,
                notes = notes,
                updatedBy = author,
                updatedAt = System.currentTimeMillis()
            )

            syncManager.queueAttendanceChange(updated, author)

            // Automatic cross-sync between Ultieme Kooi 1 en Kooi 2
            val kooi1 = findKooi1Event()
            val kooi2 = findKooi2Event()

            if (kooi1 != null && kooi2 != null) {
                if (eventId == kooi1.id) {
                    if (status == "PRESENT") {
                        // Mark in Kooi 2 as OTHER_CAGE
                        val otherExisting = db.attendanceDao().getSpecific(kooi2.id, memberId)
                        if (otherExisting == null || otherExisting.status != "PRESENT") {
                            val otherUpdated = otherExisting?.copy(
                                status = "OTHER_CAGE",
                                notes = "Deelgenomen aan Ultieme Kooi 1" + if (notes.isNotBlank()) " ($notes)" else "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            ) ?: AttendanceEntity(
                                eventId = kooi2.id,
                                memberId = memberId,
                                status = "OTHER_CAGE",
                                notes = "Deelgenomen aan Ultieme Kooi 1" + if (notes.isNotBlank()) " ($notes)" else "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            )
                            syncManager.queueAttendanceChange(otherUpdated, author)
                        }
                    } else if (status == "ABSENT") {
                        // If they were marked as OTHER_CAGE in Kooi 2 because of Kooi 1, reset to ABSENT
                        val otherExisting = db.attendanceDao().getSpecific(kooi2.id, memberId)
                        if (otherExisting != null && otherExisting.status == "OTHER_CAGE") {
                            val otherUpdated = otherExisting.copy(
                                status = "ABSENT",
                                notes = "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            )
                            syncManager.queueAttendanceChange(otherUpdated, author)
                        }
                    }
                } else if (eventId == kooi2.id) {
                    if (status == "PRESENT") {
                        // Mark in Kooi 1 as OTHER_CAGE
                        val otherExisting = db.attendanceDao().getSpecific(kooi1.id, memberId)
                        if (otherExisting == null || otherExisting.status != "PRESENT") {
                            val otherUpdated = otherExisting?.copy(
                                status = "OTHER_CAGE",
                                notes = "Deelgenomen aan Ultieme Kooi 2" + if (notes.isNotBlank()) " ($notes)" else "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            ) ?: AttendanceEntity(
                                eventId = kooi1.id,
                                memberId = memberId,
                                status = "OTHER_CAGE",
                                notes = "Deelgenomen aan Ultieme Kooi 2" + if (notes.isNotBlank()) " ($notes)" else "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            )
                            syncManager.queueAttendanceChange(otherUpdated, author)
                        }
                    } else if (status == "ABSENT") {
                        // If they were marked as OTHER_CAGE in Kooi 1 because of Kooi 2, reset to ABSENT
                        val otherExisting = db.attendanceDao().getSpecific(kooi1.id, memberId)
                        if (otherExisting != null && otherExisting.status == "OTHER_CAGE") {
                            val otherUpdated = otherExisting.copy(
                                status = "ABSENT",
                                notes = "",
                                updatedBy = author,
                                updatedAt = System.currentTimeMillis()
                            )
                            syncManager.queueAttendanceChange(otherUpdated, author)
                        }
                    }
                }
            }
        }
    }

    fun batchSetAttendance(eventId: Long, status: String) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            repository.batchSetAttendance(eventId, status, author)
            _snackBarMessage.value = "Alle leden bijgewerkt naar $status"
        }
    }

    fun applyScreenshotResults(
        targetEventId: Long,
        scannedPlayers: List<LeaderboardScannedPlayer>
    ) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            var appliedCount = 0
            val targetEvent = allEvents.value.find { it.id == targetEventId }
            val eventTitle = targetEvent?.title ?: "Evenement"

            scannedPlayers.filter { it.isSelected && it.matchedMemberId != null }.forEach { player ->
                val memberId = player.matchedMemberId ?: return@forEach
                val scoreNote = "Doelpunten: ${player.score} (#${player.rank})"
                setMemberAttendance(targetEventId, memberId, "PRESENT", scoreNote)
                appliedCount++
            }

            val syncMsg = if (isKooi1(targetEventId)) " • Kooi 2 automatisch bijgewerkt!"
                          else if (isKooi2(targetEventId)) " • Kooi 1 automatisch bijgewerkt!"
                          else ""
            _snackBarMessage.value = "✅ $appliedCount leden als AANWEZIG geregistreerd in $eventTitle$syncMsg"
            _showScreenshotDialog.value = false
        }
    }

    fun saveEvent(event: EventEntity) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            val id = repository.saveEvent(event, author)
            _selectedEventId.value = id
            _snackBarMessage.value = "Event saved."

            // If event was closed / completed, auto-send summary to Discord webhook!
            if (event.status == "COMPLETED" && _discordWebhookUrl.value.isNotBlank()) {
                DiscordWebhookService.sendRosterSummary(
                    webhookUrl = _discordWebhookUrl.value,
                    event = event,
                    members = allMembers.value,
                    attendanceList = allAttendance.value,
                    thresholdRate = _attendanceThreshold.value
                )
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            repository.deleteEvent(eventId, author)
            _selectedEventId.value = allEvents.value.firstOrNull { it.id != eventId }?.id
            _snackBarMessage.value = "Event deleted."
        }
    }

    fun saveMember(member: MemberEntity) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            repository.saveMember(member, author)
            firestoreService.syncMemberToCloud(member, author)
            _snackBarMessage.value = "Lid '${member.name}' opgeslagen & gesynchroniseerd."
        }
    }

    /**
     * Updates a member's rank (R1 to R5) with mandatory check: minimum R4 required to change.
     */
    fun updateMemberRank(member: MemberEntity, newRank: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        if (!canCurrentUserEdit()) {
            val msg = "Geen bevoegdheid: je moet minimaal rang R4 hebben om gegevens of rangen te veranderen."
            _snackBarMessage.value = msg
            onComplete?.invoke(false, msg)
            return
        }

        val author = _currentUser.value?.displayName ?: "JustineValentina"
        val oldRank = member.rank
        val updatedMember = member.copy(rank = newRank)

        viewModelScope.launch {
            // 1. Update local Room database
            repository.saveMember(updatedMember, author)

            // 2. Insert audit log
            db.auditLogDao().insert(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    authorName = author,
                    actionType = "RANK_CHANGE",
                    entityTarget = member.name,
                    details = "Rang gewijzigd van '$oldRank' naar '$newRank' door $author (Minimaal R4 vereiste voldaan)."
                )
            )

            // 3. Sync member profile and rank to Firebase Firestore
            firestoreService.updateMemberRank(member.name, newRank, author)

            val msg = "Rang van '${member.name}' succesvol veranderd naar $newRank!"
            _snackBarMessage.value = msg
            onComplete?.invoke(true, msg)
        }
    }

    fun syncAllMembersToFirestore() {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        val members = allMembers.value
        viewModelScope.launch {
            val res = firestoreService.syncAllMembersToCloud(members, author)
            if (res.isSuccess) {
                _snackBarMessage.value = "${res.getOrNull()} ledenprofielen en rangen gesynchroniseerd met Firebase Firestore!"
            } else {
                _snackBarMessage.value = "Firestore sync: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteMember(memberId: Long) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            repository.deleteMember(memberId, author)
            _snackBarMessage.value = "Member removed."
        }
    }

    fun postAppUpdate(title: String, tag: String, message: String, priority: Boolean) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            val update = AppUpdateEntity(
                title = title,
                versionOrTag = tag,
                message = message,
                publishedBy = author,
                isPriority = priority
            )
            repository.addAppUpdate(update, author)

            NotificationHelper.showNotification(
                getApplication(),
                "TED PRIVATE Update: $title",
                message
            )
            _snackBarMessage.value = "Announcement posted & alert sent!"
        }
    }

    fun triggerTestNotification() {
        NotificationHelper.showNotification(
            getApplication(),
            "TED PRIVATE Notification Test",
            "This is a verified push notification test from TED PRIVATE attendance system."
        )
        _snackBarMessage.value = "Push notification triggered!"
    }

    // PDF Import
    fun openImportDialog() { _showImportDialog.value = true }
    fun closeImportDialog() { _showImportDialog.value = false; _parsedPdfResult.value = null }

    fun parsePdfFile(uri: Uri) {
        viewModelScope.launch {
            val text = PdfImporter.extractTextFromUri(getApplication(), uri)
            val parsed = PdfImporter.parseRosterContent(text, fallbackTitle = "Imported PDF Roster")
            _parsedPdfResult.value = parsed.copy(sourceFileName = uri.lastPathSegment ?: "document.pdf")
        }
    }

    fun parsePastedRosterText(text: String, title: String) {
        val parsed = PdfImporter.parseRosterContent(text, fallbackTitle = title)
        _parsedPdfResult.value = parsed
    }

    fun confirmImportParsedData(result: ParsedPdfResult) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        viewModelScope.launch {
            val eventId = repository.importParsedData(
                eventTitle = result.eventTitle,
                eventCategory = result.eventCategory,
                eventDate = result.eventDateMillis,
                parsedRecords = result.records,
                author = author
            )
            _selectedEventId.value = eventId
            _showImportDialog.value = false
            _parsedPdfResult.value = null
            _snackBarMessage.value = "Successfully imported ${result.records.size} records into '${result.eventTitle}'!"
        }
    }

    fun exportPdfReport(): File? {
        val context = getApplication<Application>()
        val events = allEvents.value
        val members = allMembers.value
        val summaries = memberSummaries.value
        return PdfExporter.generateAndShareReport(
            context = context,
            events = events,
            members = members,
            summaries = summaries,
            periodName = _statsPeriod.value,
            groupFilter = _statsGroup.value,
            lang = _currentLanguage.value
        )
    }

    // Encrypted Database Backup & Restore
    fun createManualBackup(passphrase: String, onComplete: (Result<File>) -> Unit) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        val context = getApplication<Application>()
        viewModelScope.launch {
            val result = BackupManager.createEncryptedBackup(
                context = context,
                db = db,
                passphrase = passphrase,
                isAutomated = false,
                author = author
            )
            onComplete(result)
        }
    }

    fun restoreBackup(file: File, passphrase: String, onComplete: (Boolean, String) -> Unit) {
        val author = _currentUser.value?.displayName ?: "JustineValentina"
        val context = getApplication<Application>()
        viewModelScope.launch {
            val result = BackupManager.restoreEncryptedBackup(
                context = context,
                db = db,
                backupFile = file,
                passphrase = passphrase,
                author = author
            )
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _snackBarMessage.value = "Backup succesvol hersteld ($count records)!"
                onComplete(true, "Backup succesvol hersteld ($count records)!")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Onbekende fout bij herstellen"
                _snackBarMessage.value = "Fout bij herstellen: $err"
                onComplete(false, "Fout: $err")
            }
        }
    }
}
