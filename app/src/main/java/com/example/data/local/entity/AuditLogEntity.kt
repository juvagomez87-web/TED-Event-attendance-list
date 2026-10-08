package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val authorName: String,
    val actionType: String, // ATTENDANCE_CHANGE, EVENT_CREATE, EVENT_UPDATE, MEMBER_ADD, PDF_IMPORT, BATCH_UPDATE, NOTE_EDIT, SYNC_RESOLVE
    val entityTarget: String,
    val details: String,
    // Offline sync fields
    val syncStatus: String = "SYNCED" // SYNCED, PENDING_UPLOAD
)
