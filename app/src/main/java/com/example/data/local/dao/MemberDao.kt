package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("""
        SELECT * FROM members 
        ORDER BY 
            CASE 
                WHEN UPPER(TRIM(rank)) = 'R4/R5' THEN 1
                WHEN UPPER(TRIM(rank)) = 'R5' THEN 2
                WHEN UPPER(TRIM(rank)) = 'R4' THEN 3
                WHEN UPPER(TRIM(rank)) = 'R3' THEN 4
                WHEN UPPER(TRIM(rank)) = 'R2' THEN 5
                WHEN UPPER(TRIM(rank)) = 'R1' THEN 6
                ELSE 7
            END ASC,
            id ASC
    """)
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Query("SELECT * FROM members WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun update(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM members")
    suspend fun count(): Int
}
