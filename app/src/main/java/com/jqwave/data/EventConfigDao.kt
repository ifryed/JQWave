package com.jqwave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventConfigDao {
    @Query("SELECT * FROM event_configs")
    fun observeAll(): Flow<List<EventConfigEntity>>

    @Query("SELECT * FROM event_configs")
    suspend fun getAll(): List<EventConfigEntity>

    @Query("SELECT * FROM event_configs WHERE kind = :kind LIMIT 1")
    suspend fun getByKind(kind: String): EventConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EventConfigEntity)
}
