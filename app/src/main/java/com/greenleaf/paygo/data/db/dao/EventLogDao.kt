package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.greenleaf.paygo.data.db.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Insert
    suspend fun insert(event: EventLogEntity)

    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_log ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<EventLogEntity>
}
