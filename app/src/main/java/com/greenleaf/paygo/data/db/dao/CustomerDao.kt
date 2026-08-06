package com.greenleaf.paygo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greenleaf.paygo.data.db.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: CustomerEntity)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE custId = :custId")
    suspend fun getById(custId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE seq = :seq")
    suspend fun getBySeq(seq: Int): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): CustomerEntity?

    @Query("SELECT COALESCE(MAX(seq), 0) FROM customers")
    suspend fun maxSeq(): Int

    @Query("SELECT * FROM customers ORDER BY seq ASC")
    suspend fun getAllOnce(): List<CustomerEntity>

    @Query("SELECT COUNT(*) FROM customers")
    fun observeCount(): Flow<Int>
}
