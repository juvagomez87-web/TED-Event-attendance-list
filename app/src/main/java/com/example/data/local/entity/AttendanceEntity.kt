package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["eventId", "memberId"], unique = true),
        Index(value = ["eventId"]),
        Index(value = ["memberId"]),
        Index(value = ["syncStatus"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: Long,
    val memberId: Long,
    val status: String = "ABSENT", // PRESENT, ABSENT, LATE, EXCUSED
    val notes: String = "",
    val updatedBy: String = "Admin",
    val updatedAt: Long = System.currentTimeMillis(),
    // Offline sync fields
    val syncStatus: String = "SYNCED", // SYNCED, PENDING_UPLOAD, CONFLICT
    val version: Long = 1L,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val conflictServerStatus: String? = null,
    val conflictServerNotes: String? = null,
    val conflictServerUpdatedBy: String? = null
)
