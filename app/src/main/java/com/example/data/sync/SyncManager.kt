package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ConflictResolutionPolicy {
    LAST_WRITE_WINS,
    ADMIN_PRIORITY,
    MANUAL_REVIEW
}

data class ConflictItem(
    val attendance: AttendanceEntity,
    val memberName: String,
    val eventTitle: String,
    val localStatus: String,
    val localNotes: String,
    val serverStatus: String,
    val serverNotes: String,
    val serverUpdatedBy: String
)

class SyncManager(
    private val context: Context,
    private val db: AppDatabase,
    private val scope: CoroutineScope
) {
    private val attendanceDao = db.attendanceDao()
    private val auditLogDao = db.auditLogDao()
    private val memberDao = db.memberDao()
    private val eventDao = db.eventDao()
    private val syncQueueDao = db.syncQueueDao()

    private val _isNetworkAvailable = MutableStateFlow(true)
    private val _isSimulatedOffline = MutableStateFlow(false)

    // Effective online state
    val isOnline: StateFlow<Boolean> = MutableStateFlow(true)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _conflictPolicy = MutableStateFlow(ConflictResolutionPolicy.MANUAL_REVIEW)
    val conflictPolicy: StateFlow<ConflictResolutionPolicy> = _conflictPolicy.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        monitorNetwork()
    }

    private fun monitorNetwork() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
                updateOnlineState()
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = false
                updateOnlineState()
            }
        })
    }

    fun setSimulatedOffline(offline: Boolean) {
        _isSimulatedOffline.value = offline
        updateOnlineState()
    }

    fun setConflictPolicy(policy: ConflictResolutionPolicy) {
        _conflictPolicy.value = policy
    }

    private fun updateOnlineState() {
        val effective = _isNetworkAvailable.value && !_isSimulatedOffline.value
        (isOnline as MutableStateFlow).value = effective
        if (effective) {
            // Auto-trigger sync on reconnecting
            scope.launch(Dispatchers.IO) {
                syncPendingChanges("AutoSync")
            }
        }
    }

    suspend fun queueAttendanceChange(
        attendance: AttendanceEntity,
        author: String
    ) {
        val online = isOnline.value
        val syncStatus = if (online) "SYNCED" else "PENDING_UPLOAD"

        val updatedAttendance = attendance.copy(
            syncStatus = syncStatus,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.upsert(updatedAttendance)

        if (!online) {
            val payload = JSONObject().apply {
                put("eventId", attendance.eventId)
                put("memberId", attendance.memberId)
                put("status", attendance.status)
                put("notes", attendance.notes)
                put("updatedBy", author)
                put("updatedAt", updatedAttendance.updatedAt)
            }.toString()

            syncQueueDao.insert(
                SyncQueueEntity(
                    entityType = "ATTENDANCE",
                    entityId = updatedAttendance.id,
                    operation = "UPSERT",
                    payloadJson = payload,
                    author = author,
                    syncState = "PENDING"
                )
            )
        }
    }

    suspend fun syncPendingChanges(triggerAuthor: String = "SyncManager"): Int {
        if (!isOnline.value) {
            _syncMessage.value = "Cannot sync while offline. Changes are saved locally."
            return 0
        }

        _isSyncing.value = true
        var syncedCount = 0

        try {
            val pendingItems = syncQueueDao.getPendingItems().first()

            for (item in pendingItems) {
                if (item.entityType == "ATTENDANCE") {
                    val payload = JSONObject(item.payloadJson)
                    val eventId = payload.getLong("eventId")
                    val memberId = payload.getLong("memberId")
                    val localStatus = payload.getString("status")
                    val localNotes = payload.getString("notes")
                    val localAuthor = payload.getString("updatedBy")

                    // Simulate server verification check:
                    // If conflict simulated on this attendance, trigger conflict handling
                    val currentAttendance = attendanceDao.getSpecific(eventId, memberId)
                    if (currentAttendance?.syncStatus == "CONFLICT") {
                        // Keep in conflict state
                        continue
                    }

                    // Successfully synchronized
                    currentAttendance?.let {
                        attendanceDao.updateSyncStatus(it.id, "SYNCED")
                    }
                    syncQueueDao.updateState(item.id, "RESOLVED")
                    syncedCount++
                }
            }

            syncQueueDao.clearResolved()
            _lastSyncTimestamp.value = System.currentTimeMillis()

            if (syncedCount > 0) {
                auditLogDao.insert(
                    AuditLogEntity(
                        authorName = triggerAuthor,
                        actionType = "SYNC_RESOLVE",
                        entityTarget = "Cloud & Local Sync",
                        details = "Synchronized $syncedCount pending offline changes to server database.",
                        syncStatus = "SYNCED"
                    )
                )
                _syncMessage.value = "Synced $syncedCount offline changes successfully!"
            } else {
                _syncMessage.value = "All records are up to date."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _syncMessage.value = "Sync error: ${e.message}"
        } finally {
            _isSyncing.value = false
        }

        return syncedCount
    }

    suspend fun simulateServerConflict(eventId: Long, memberId: Long) {
        val attendance = attendanceDao.getSpecific(eventId, memberId) ?: return
        val conflictingServerStatus = if (attendance.status == "PRESENT") "ABSENT" else "PRESENT"
        val conflictingServerNotes = "Server/iOS coordinator marked as $conflictingServerStatus (10m ago)"
        val serverAuthor = "Officer Sarah (iOS Client)"

        when (_conflictPolicy.value) {
            ConflictResolutionPolicy.LAST_WRITE_WINS -> {
                // Auto-resolve: local was written later, so local wins
                attendanceDao.updateSyncStatus(attendance.id, "SYNCED")
                auditLogDao.insert(
                    AuditLogEntity(
                        authorName = "SyncManager (LWW)",
                        actionType = "SYNC_RESOLVE",
                        entityTarget = "Attendance ID#${attendance.id}",
                        details = "Conflict auto-resolved via Last-Write-Wins: Kept local '${attendance.status}' over server '$conflictingServerStatus'.",
                        syncStatus = "SYNCED"
                    )
                )
                _syncMessage.value = "Conflict auto-resolved with Last-Write-Wins policy."
            }
            ConflictResolutionPolicy.ADMIN_PRIORITY -> {
                // Admin takes priority
                attendanceDao.updateSyncStatus(attendance.id, "SYNCED")
                auditLogDao.insert(
                    AuditLogEntity(
                        authorName = "SyncManager (Admin Priority)",
                        actionType = "SYNC_RESOLVE",
                        entityTarget = "Attendance ID#${attendance.id}",
                        details = "Conflict auto-resolved via Admin Priority: Local admin edit preserved.",
                        syncStatus = "SYNCED"
                    )
                )
                _syncMessage.value = "Conflict auto-resolved via Admin Priority."
            }
            ConflictResolutionPolicy.MANUAL_REVIEW -> {
                // Flag as conflict for manual admin resolution
                val conflictAttendance = attendance.copy(
                    syncStatus = "CONFLICT",
                    conflictServerStatus = conflictingServerStatus,
                    conflictServerNotes = conflictingServerNotes,
                    conflictServerUpdatedBy = serverAuthor
                )
                attendanceDao.upsert(conflictAttendance)

                syncQueueDao.insert(
                    SyncQueueEntity(
                        entityType = "ATTENDANCE",
                        entityId = attendance.id,
                        operation = "CONFLICT",
                        payloadJson = JSONObject().apply {
                            put("localStatus", attendance.status)
                            put("localNotes", attendance.notes)
                        }.toString(),
                        author = serverAuthor,
                        syncState = "CONFLICT",
                        conflictReason = "Concurrent edit on iOS client by $serverAuthor"
                    )
                )

                auditLogDao.insert(
                    AuditLogEntity(
                        authorName = "SyncManager",
                        actionType = "CONFLICT_DETECTED",
                        entityTarget = "Attendance ID#${attendance.id}",
                        details = "Concurrent edit detected: Local [${attendance.status}] vs Server [$conflictingServerStatus]. Awaiting manual review.",
                        syncStatus = "SYNCED"
                    )
                )
                _syncMessage.value = "Conflict detected! Please review in Sync Manager."
            }
        }
    }

    suspend fun resolveConflictManually(
        attendanceId: Long,
        chosenStatus: String,
        chosenNotes: String,
        author: String,
        resolutionType: String // KEEP_LOCAL, KEEP_SERVER, MERGE
    ) {
        val attendance = attendanceDao.getById(attendanceId) ?: return
        val member = memberDao.getMemberById(attendance.memberId)
        val event = eventDao.getEventById(attendance.eventId)

        attendanceDao.resolveConflict(
            id = attendanceId,
            status = chosenStatus,
            notes = chosenNotes,
            updatedBy = author,
            updatedAt = System.currentTimeMillis()
        )

        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "SYNC_RESOLVE",
                entityTarget = "${member?.name ?: "Member"} (${event?.title ?: "Event"})",
                details = "Manual conflict resolution ($resolutionType): Applied status '$chosenStatus' with note: '$chosenNotes'.",
                syncStatus = "SYNCED"
            )
        )

        _syncMessage.value = "Conflict resolved successfully ($resolutionType)."
    }

    suspend fun exportCrossPlatformSyncBundle(): String {
        val events = eventDao.getAllEvents().first()
        val members = memberDao.getAllMembers().first()
        val attendances = attendanceDao.getAllAttendance().first()
        val logs = auditLogDao.getAllLogs().first()

        val root = JSONObject()
        root.put("app", "TED PRIVATE")
        root.put("version", "2.6.0")
        root.put("timestamp", System.currentTimeMillis())

        val eventsArr = JSONArray()
        events.forEach { ev ->
            eventsArr.put(JSONObject().apply {
                put("id", ev.id)
                put("title", ev.title)
                put("category", ev.category)
                put("dateEpochMillis", ev.dateEpochMillis)
                put("status", ev.status)
            })
        }
        root.put("events", eventsArr)

        val membersArr = JSONArray()
        members.forEach { m ->
            membersArr.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("handle", m.handle)
                put("rank", m.rank)
                put("groupRole", m.groupRole)
            })
        }
        root.put("members", membersArr)

        val attendancesArr = JSONArray()
        attendances.forEach { a ->
            attendancesArr.put(JSONObject().apply {
                put("eventId", a.eventId)
                put("memberId", a.memberId)
                put("status", a.status)
                put("notes", a.notes)
                put("updatedBy", a.updatedBy)
                put("updatedAt", a.updatedAt)
            })
        }
        root.put("attendance", attendancesArr)

        return root.toString(2)
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
