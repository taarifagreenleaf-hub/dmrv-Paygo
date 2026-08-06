package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.greenleaf.paygo.data.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Update
    suspend fun update(message: MessageEntity)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE category = :category ORDER BY timestamp DESC")
    fun observeByCategory(category: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE groupKey = :groupKey ORDER BY timestamp DESC")
    fun observeByGroup(groupKey: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeByCustomer(customerId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: Long): MessageEntity?

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    fun observeCount(): Flow<Int>
}
