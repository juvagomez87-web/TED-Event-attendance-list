package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // ATTENDANCE, AUDIT_LOG, EVENT, MEMBER
    val entityId: Long,
    val operation: String, // UPSERT, DELETE
    val payloadJson: String,
    val clientTimestamp: Long = System.currentTimeMillis(),
    val author: String,
    val syncState: String = "PENDING", // PENDING, SYNCING, CONFLICT, RESOLVED, FAILED
    val retryCount: Int = 0,
    val serverPayloadJson: String? = null,
    val conflictReason: String? = null
)
