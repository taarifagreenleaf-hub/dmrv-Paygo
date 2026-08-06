package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greenleaf.paygo.data.db.entity.ContactGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: ContactGroupEntity)

    @Query("SELECT * FROM contact_groups ORDER BY lastMessageAt DESC")
    fun observeAll(): Flow<List<ContactGroupEntity>>

    @Query("SELECT * FROM contact_groups WHERE groupKey = :key")
    suspend fun getByKey(key: String): ContactGroupEntity?
}
