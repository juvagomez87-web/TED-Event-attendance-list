package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AppUpdateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUpdateDao {
    @Query("SELECT * FROM app_updates ORDER BY timestamp DESC")
    fun getAllUpdates(): Flow<List<AppUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(update: AppUpdateEntity): Long

    @Query("DELETE FROM app_updates WHERE id = :id")
    suspend fun deleteById(id: Long)
}
