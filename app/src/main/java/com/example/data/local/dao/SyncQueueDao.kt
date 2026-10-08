package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE syncState = 'PENDING' ORDER BY clientTimestamp ASC")
    fun getPendingItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE syncState = 'CONFLICT' ORDER BY clientTimestamp DESC")
    fun getConflictItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY clientTimestamp DESC")
    fun getAllItems(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncState = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncState = 'CONFLICT'")
    fun getConflictCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET syncState = :newState WHERE id = :id")
    suspend fun updateState(id: Long, newState: String)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM sync_queue WHERE syncState = 'RESOLVED'")
    suspend fun clearResolved()
}
