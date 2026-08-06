package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Audit trail of every automated action (reply / forward) the app performs. */
@Entity(tableName = "event_log", indices = [Index("timestamp")])
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long?,
    /** REPLY_SMS, REPLY_WHATSAPP, FORWARD_SMS, FORWARD_WHATSAPP, ERROR */
    val action: String,
    val target: String?,
    val detail: String,
    val success: Boolean,
    val timestamp: Long
)
