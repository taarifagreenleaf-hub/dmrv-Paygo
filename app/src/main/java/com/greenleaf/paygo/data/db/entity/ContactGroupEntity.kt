package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rolls up all messages that share a [groupKey] (normalised payee/sender) so the
 * user can track a running relationship: how much a payee has sent in total,
 * when they last messaged, etc.
 */
@Entity(tableName = "contact_groups")
data class ContactGroupEntity(
    @PrimaryKey val groupKey: String,
    val displayName: String,
    val lastMessageAt: Long = 0,
    val messageCount: Int = 0,
    val totalReceived: Double = 0.0,
    val note: String? = null
)
