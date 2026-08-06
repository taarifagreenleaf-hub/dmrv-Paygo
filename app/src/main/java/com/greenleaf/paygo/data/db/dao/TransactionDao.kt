package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(txn: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE provider = :provider ORDER BY timestamp DESC")
    fun observeByProvider(provider: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE direction = 'RECEIVED'")
    fun observeTotalReceived(): Flow<Double>

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE messageId = :messageId LIMIT 1")
    suspend fun getByMessageId(messageId: Long): TransactionEntity?
}
