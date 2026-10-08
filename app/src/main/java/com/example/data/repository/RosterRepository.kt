package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppUpdateEntity
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class MemberAttendanceSummary(
    val member: MemberEntity,
    val totalEvents: Int,
    val attendedCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val absentCount: Int,
    val attendanceRate: Float
)

data class EventAttendanceItem(
    val member: MemberEntity,
    val attendance: AttendanceEntity?
)

data class ParsedMemberRecord(
    val name: String,
    val handle: String = "",
    val rank: String = "R4",
    val role: String = "Member",
    val status: String = "PRESENT", // PRESENT, ABSENT, LATE, EXCUSED
    val notes: String = ""
)

class RosterRepository(private val db: AppDatabase) {
    private val memberDao = db.memberDao()
    private val eventDao = db.eventDao()
    private val attendanceDao = db.attendanceDao()
    private val auditLogDao = db.auditLogDao()
    private val appUpdateDao = db.appUpdateDao()
    private val userAccountDao = db.userAccountDao()

    val allMembers: Flow<List<MemberEntity>> = memberDao.getAllMembers()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val allAttendance: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendance()
    val allAuditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    val allAppUpdates: Flow<List<AppUpdateEntity>> = appUpdateDao.getAllUpdates()
    val allAccounts: Flow<List<UserAccountEntity>> = userAccountDao.getAllAccounts()

    fun getAttendanceForEvent(eventId: Long): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceForEvent(eventId)
    }

    // User authentication & activity tracking
    suspend fun login(username: String, pin: String): UserAccountEntity? {
        val account = userAccountDao.getByUsername(username.trim())
        if (account != null && account.pinOrPassword == pin.trim()) {
            val now = System.currentTimeMillis()
            userAccountDao.recordLogin(account.id, now)
            auditLogDao.insert(
                AuditLogEntity(
                    authorName = account.displayName,
                    actionType = "USER_LOGIN",
                    entityTarget = "Session: ${account.username}",
                    details = "User logged in successfully (Role: ${account.role})."
                )
            )
            return account.copy(lastLoginAt = now, loginCount = account.loginCount + 1)
        }
        return null
    }

    suspend fun register(
        username: String,
        displayName: String,
        emailOrDiscord: String,
        pin: String,
        role: String
    ): Result<UserAccountEntity> {
        val cleanUsername = username.trim()
        val existing = userAccountDao.getByUsername(cleanUsername)
        if (existing != null) {
            return Result.failure(Exception("Username already exists"))
        }

        // Also check or link with member
        val member = memberDao.findByName(displayName.trim())
        val newAccount = UserAccountEntity(
            username = cleanUsername,
            displayName = displayName.trim(),
            emailOrDiscord = emailOrDiscord.trim(),
            pinOrPassword = pin.trim(),
            role = role,
            memberId = member?.id,
            lastLoginAt = System.currentTimeMillis(),
            loginCount = 1
        )
        val newId = userAccountDao.insert(newAccount)
        val created = newAccount.copy(id = newId)

        auditLogDao.insert(
            AuditLogEntity(
                authorName = displayName,
                actionType = "ACCOUNT_REGISTER",
                entityTarget = "User: $cleanUsername",
                details = "Registered personal account with role $role."
            )
        )
        return Result.success(created)
    }

    suspend fun setAttendance(
        eventId: Long,
        memberId: Long,
        status: String,
        notes: String,
        author: String
    ) {
        val existing = attendanceDao.getSpecific(eventId, memberId)
        val member = memberDao.getMemberById(memberId)
        val event = eventDao.getEventById(eventId)

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
        attendanceDao.upsert(updated)

        val memberName = member?.name ?: "ID#$memberId"
        val eventTitle = event?.title ?: "Event#$eventId"
        val oldStatus = existing?.status ?: "NONE"
        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "ATTENDANCE_CHANGE",
                entityTarget = "$memberName ($eventTitle)",
                details = "Changed status from $oldStatus to $status. Note: '${if (notes.isNotBlank()) notes else "None"}'"
            )
        )
    }

    suspend fun batchSetAttendance(
        eventId: Long,
        status: String,
        author: String
    ) {
        val event = eventDao.getEventById(eventId) ?: return
        val membersList = memberDao.getAllMembers().first()
        membersList.forEach { member ->
            val existing = attendanceDao.getSpecific(eventId, member.id)
            val updated = existing?.copy(
                status = status,
                updatedBy = author,
                updatedAt = System.currentTimeMillis()
            ) ?: AttendanceEntity(
                eventId = eventId,
                memberId = member.id,
                status = status,
                updatedBy = author,
                updatedAt = System.currentTimeMillis()
            )
            attendanceDao.upsert(updated)
        }

        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "BATCH_UPDATE",
                entityTarget = "Event: ${event.title}",
                details = "Batch updated all members to status '$status'."
            )
        )
    }

    suspend fun saveEvent(event: EventEntity, author: String): Long {
        val isNew = event.id == 0L
        val id = if (isNew) {
            val newId = eventDao.insert(event)
            auditLogDao.insert(
                AuditLogEntity(
                    authorName = author,
                    actionType = "EVENT_CREATE",
                    entityTarget = "Event: ${event.title}",
                    details = "Created new event on date ${event.dateEpochMillis} in category ${event.category}."
                )
            )
            newId
        } else {
            eventDao.update(event)
            auditLogDao.insert(
                AuditLogEntity(
                    authorName = author,
                    actionType = "EVENT_UPDATE",
                    entityTarget = "Event: ${event.title}",
                    details = "Updated event details, status: ${event.status}."
                )
            )
            event.id
        }
        return id
    }

    suspend fun deleteEvent(eventId: Long, author: String) {
        val event = eventDao.getEventById(eventId)
        eventDao.deleteById(eventId)
        attendanceDao.deleteForEvent(eventId)
        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "EVENT_DELETE",
                entityTarget = "Event: ${event?.title ?: "#$eventId"}",
                details = "Deleted event and all associated attendance records."
            )
        )
    }

    suspend fun saveMember(member: MemberEntity, author: String): Long {
        val isNew = member.id == 0L
        val id = if (isNew) {
            val newId = memberDao.insert(member)
            auditLogDao.insert(
                AuditLogEntity(
                    authorName = author,
                    actionType = "MEMBER_ADD",
                    entityTarget = "Member: ${member.name}",
                    details = "Added new member with rank ${member.rank} and group role ${member.groupRole}."
                )
            )
            newId
        } else {
            memberDao.update(member)
            auditLogDao.insert(
                AuditLogEntity(
                    authorName = author,
                    actionType = "MEMBER_UPDATE",
                    entityTarget = "Member: ${member.name}",
                    details = "Updated member details, rank: ${member.rank}, role: ${member.groupRole}."
                )
            )
            member.id
        }
        return id
    }

    suspend fun deleteMember(memberId: Long, author: String) {
        val member = memberDao.getMemberById(memberId)
        memberDao.deleteById(memberId)
        attendanceDao.deleteForMember(memberId)
        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "MEMBER_DELETE",
                entityTarget = "Member: ${member?.name ?: "#$memberId"}",
                details = "Deleted member and associated historical attendance records."
            )
        )
    }

    suspend fun addAppUpdate(update: AppUpdateEntity, author: String): Long {
        val id = appUpdateDao.insert(update)
        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "APP_UPDATE",
                entityTarget = "Update: ${update.title}",
                details = "Published new update/announcement (${update.versionOrTag}): ${update.message}"
            )
        )
        return id
    }

    suspend fun importParsedData(
        eventTitle: String,
        eventCategory: String,
        eventDate: Long,
        parsedRecords: List<ParsedMemberRecord>,
        author: String
    ): Long {
        val existingEvent = eventDao.findByTitle(eventTitle)
        val eventId = existingEvent?.id ?: eventDao.insert(
            EventEntity(
                title = eventTitle,
                category = eventCategory,
                description = "Imported from PDF/Document file",
                dateEpochMillis = eventDate,
                status = "ACTIVE",
                createdBy = author
            )
        )

        var newMembersCount = 0
        var attendanceCount = 0

        parsedRecords.forEach { record ->
            var member = memberDao.findByName(record.name)
            if (member == null) {
                val memberId = memberDao.insert(
                    MemberEntity(
                        name = record.name,
                        handle = if (record.handle.isNotBlank()) record.handle else "@${record.name.lowercase().replace(" ", "")}",
                        rank = if (record.rank.isNotBlank()) record.rank else "R4",
                        groupRole = if (record.role.isNotBlank()) record.role else "Member",
                        notes = record.notes
                    )
                )
                member = memberDao.getMemberById(memberId)
                newMembersCount++
            }

            if (member != null) {
                val existingAttendance = attendanceDao.getSpecific(eventId, member.id)
                val newAttendance = existingAttendance?.copy(
                    status = record.status,
                    notes = if (record.notes.isNotBlank()) record.notes else existingAttendance.notes,
                    updatedBy = author,
                    updatedAt = System.currentTimeMillis()
                ) ?: AttendanceEntity(
                    eventId = eventId,
                    memberId = member.id,
                    status = record.status,
                    notes = record.notes,
                    updatedBy = author,
                    updatedAt = System.currentTimeMillis()
                )
                attendanceDao.upsert(newAttendance)
                attendanceCount++
            }
        }

        auditLogDao.insert(
            AuditLogEntity(
                authorName = author,
                actionType = "PDF_IMPORT",
                entityTarget = "Event: $eventTitle",
                details = "Imported PDF data: processed $attendanceCount attendance records ($newMembersCount new members created)."
            )
        )

        return eventId
    }
}
