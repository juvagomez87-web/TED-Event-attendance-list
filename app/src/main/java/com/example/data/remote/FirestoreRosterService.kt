package com.example.data.remote

import com.example.data.local.entity.MemberEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreRosterService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val membersCollection = firestore.collection("member_profiles")

    /**
     * Saves or updates a member's profile and rank in Cloud Firestore.
     */
    suspend fun syncMemberToCloud(member: MemberEntity, updatedBy: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = member.name.replace("/", "_").trim()
            val profile = MemberProfile(
                id = docId,
                name = member.name,
                rank = member.rank,
                groupRole = member.groupRole,
                handle = member.handle,
                notes = member.notes,
                active = member.active,
                updatedBy = updatedBy,
                updatedAt = System.currentTimeMillis()
            )
            membersCollection.document(docId).set(profile, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates only a member's rank and updatedBy in Cloud Firestore.
     */
    suspend fun updateMemberRank(memberName: String, newRank: String, updatedBy: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docId = memberName.replace("/", "_").trim()
            membersCollection.document(docId).set(
                mapOf(
                    "name" to memberName,
                    "rank" to newRank,
                    "updatedBy" to updatedBy,
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Batch uploads all local member profiles and ranks to Cloud Firestore.
     */
    suspend fun syncAllMembersToCloud(members: List<MemberEntity>, updatedBy: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            members.forEach { m ->
                val docId = m.name.replace("/", "_").trim()
                val docRef = membersCollection.document(docId)
                val profile = MemberProfile(
                    id = docId,
                    name = m.name,
                    rank = m.rank,
                    groupRole = m.groupRole,
                    handle = m.handle,
                    notes = m.notes,
                    active = m.active,
                    updatedBy = updatedBy,
                    updatedAt = System.currentTimeMillis()
                )
                batch.set(docRef, profile, SetOptions.merge())
            }
            batch.commit().await()
            Result.success(members.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
