package com.example.data.remote

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore data model for storing member profiles and their ranks in Cloud Firestore.
 */
data class MemberProfile(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val rank: String = "R3",
    @get:PropertyName("groupRole") @set:PropertyName("groupRole")
    var groupRole: String = "Member",
    val handle: String = "",
    val notes: String = "",
    val active: Boolean = true,
    @get:PropertyName("updatedBy") @set:PropertyName("updatedBy")
    var updatedBy: String = "JustineValentina",
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Long = System.currentTimeMillis()
)
