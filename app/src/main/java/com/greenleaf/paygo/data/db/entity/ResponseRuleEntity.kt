package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A "training" rule that tells the app what to reply and how. Templates support
 * placeholders that are filled from the parsed message/transaction:
 *   {name} {amount} {currency} {provider} {reference} {number} {balance} {date}
 */
@Entity(tableName = "response_rules")
data class ResponseRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val name: String,
    val enabled: Boolean = true,
    val priority: Int = 0,

    /** PAYMENT, NORMAL or KEYWORD — see [ResponseTrigger]. */
    val triggerType: String = ResponseTrigger.PAYMENT.name,

    /** Only fire for this provider slug (null = any). */
    val providerFilter: String? = null,
    /** For KEYWORD triggers: comma separated keywords matched in the body. */
    val keywords: String? = null,
    /** Amount bounds for PAYMENT triggers (null = unbounded). */
    val minAmount: Double? = null,
    val maxAmount: Double? = null,

    val replyViaSms: Boolean = true,
    val replyViaWhatsApp: Boolean = false,

    /** The reply text template. */
    val template: String
)

enum class ResponseTrigger { PAYMENT, NORMAL, KEYWORD }
