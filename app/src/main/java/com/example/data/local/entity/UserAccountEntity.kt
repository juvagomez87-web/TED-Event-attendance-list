package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val displayName: String,
    val emailOrDiscord: String,
    val pinOrPassword: String,
    val role: String = "MEMBER", // ADMIN, OFFICER, MEMBER
    val memberId: Long? = null,
    val lastLoginAt: Long = 0L,
    val loginCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
