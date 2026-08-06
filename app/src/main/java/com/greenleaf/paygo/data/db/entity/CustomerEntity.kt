package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A tracked customer. Created the first time we receive a payment from a phone
 * number and given a human-friendly [custId] (the "sms-cust-id", e.g. CUST-0001)
 * that we ask the customer to quote when they reply. Subsequent replies that
 * mention the id are linked back here and fill in name/location/product.
 */
@Entity(tableName = "customers", indices = [Index("phoneNumber"), Index("seq", unique = true)])
data class CustomerEntity(
    @PrimaryKey val custId: String,
    /** Numeric sequence behind [custId], used for lookup when a customer quotes it. */
    val seq: Int,
    val phoneNumber: String? = null,
    val name: String? = null,
    val location: String? = null,
    val productType: String? = null,
    val totalPaid: Double = 0.0,
    val lastAmount: Double? = null,
    val paymentCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
