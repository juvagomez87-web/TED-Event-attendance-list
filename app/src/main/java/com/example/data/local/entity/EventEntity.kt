package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Raid",
    val description: String = "",
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val targetGroup: String = "All",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, UPCOMING
    val locationOrChannel: String = "Discord Voice 1",
    val createdBy: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)
