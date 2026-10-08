package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE eventId = :eventId")
    fun getAttendanceForEvent(eventId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE memberId = :memberId")
    fun getAttendanceForMember(memberId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE syncStatus = 'PENDING_UPLOAD'")
    fun getPendingAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE syncStatus = 'CONFLICT'")
    fun getConflictAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE eventId = :eventId AND memberId = :memberId LIMIT 1")
    suspend fun getSpecific(eventId: Long, memberId: Long): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: AttendanceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attendances: List<AttendanceEntity>)

    @Update
    suspend fun update(attendance: AttendanceEntity)

    @Query("UPDATE attendance SET syncStatus = :status, lastSyncTimestamp = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE attendance SET status = :status, notes = :notes, updatedBy = :updatedBy, updatedAt = :updatedAt, syncStatus = 'SYNCED', conflictServerStatus = NULL, conflictServerNotes = NULL WHERE id = :id")
    suspend fun resolveConflict(id: Long, status: String, notes: String, updatedBy: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM attendance WHERE eventId = :eventId")
    suspend fun deleteForEvent(eventId: Long)

    @Query("DELETE FROM attendance WHERE memberId = :memberId")
    suspend fun deleteForMember(memberId: Long)
}
