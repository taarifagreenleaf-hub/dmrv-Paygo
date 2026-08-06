package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Every incoming SMS is stored here verbatim. Payment messages are additionally
 * parsed into a [TransactionEntity]; "normal" messages are grouped by [groupKey]
 * so a conversation with a single payee/sender can be tracked over time.
 */
@Entity(
    tableName = "messages",
    indices = [Index("groupKey"), Index("timestamp"), Index("category")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Raw sender line as delivered by the network, e.g. "MPESA", "HaloPesa", "+2557..." */
    val address: String,

    /** Full SMS text. */
    val body: String,

    /** Epoch millis the SMS was received. */
    val timestamp: Long,

    /** Which SIM/subscription received it (-1 if unknown / single SIM). */
    val simSubscriptionId: Int = -1,

    /** PAYMENT, NORMAL or UNKNOWN — see [MessageCategory]. */
    val category: String = MessageCategory.UNKNOWN.name,

    /** Detected payment provider slug (mpesa, mixx, halopesa, ...) or null. */
    val provider: String? = null,

    /** Normalised grouping key (payee/sender) used to bucket conversations. */
    val groupKey: String,

    /** Key info captured from a free-form message, stored as a JSON object
     *  (e.g. {"name":"Asha","location":"Mbeya","amount":"50000","system_size":"200W"}). */
    val extractedInfo: String? = null,

    /** The sms-cust-id this message is linked to, if any (from payment or a reply
     *  that quoted the id). */
    val customerId: String? = null,

    /** True once a response rule has processed this message. */
    val handled: Boolean = false,
    val repliedSms: Boolean = false,
    val repliedWhatsApp: Boolean = false,
    val forwarded: Boolean = false
)

enum class MessageCategory { PAYMENT, NORMAL, UNKNOWN }
