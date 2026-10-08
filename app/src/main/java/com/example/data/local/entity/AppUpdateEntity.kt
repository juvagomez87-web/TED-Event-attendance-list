package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_updates")
data class AppUpdateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val versionOrTag: String = "v1.2.0",
    val message: String,
    val publishedBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val isPriority: Boolean = false
)
