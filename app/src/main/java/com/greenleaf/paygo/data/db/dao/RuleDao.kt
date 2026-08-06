package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParsingRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ParsingRuleEntity): Long

    @Update
    suspend fun update(rule: ParsingRuleEntity)

    @Delete
    suspend fun delete(rule: ParsingRuleEntity)

    @Query("SELECT * FROM parsing_rules ORDER BY priority DESC, id ASC")
    fun observeAll(): Flow<List<ParsingRuleEntity>>

    @Query("SELECT * FROM parsing_rules WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getEnabled(): List<ParsingRuleEntity>

    @Query("SELECT COUNT(*) FROM parsing_rules")
    suspend fun count(): Int
}

@Dao
interface ResponseRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ResponseRuleEntity): Long

    @Update
    suspend fun update(rule: ResponseRuleEntity)

    @Delete
    suspend fun delete(rule: ResponseRuleEntity)

    @Query("SELECT * FROM response_rules ORDER BY priority DESC, id ASC")
    fun observeAll(): Flow<List<ResponseRuleEntity>>

    @Query("SELECT * FROM response_rules WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun getEnabled(): List<ResponseRuleEntity>

    @Query("SELECT COUNT(*) FROM response_rules")
    suspend fun count(): Int
}
