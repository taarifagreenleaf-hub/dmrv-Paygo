package com.greenleaf.paygo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A payment extracted from a payment SMS (M-Pesa, Mixx by Yas, HaloPesa, T-Pesa,
 * Selcom, CRDB, NMB, ...). One transaction maps back to one [MessageEntity].
 */
@Entity(
    tableName = "transactions",
    indices = [Index("messageId", unique = true), Index("provider"), Index("timestamp"), Index("senderNumber")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val messageId: Long,

    /** Provider slug: mpesa, mixx, halopesa, tpesa, selcom, crdb, nmb, airtelmoney, ... */
    val provider: String,

    /** RECEIVED (money in) or SENT (money out). */
    val direction: String = TxnDirection.RECEIVED.name,

    val amount: Double,
    val currency: String = "TZS",

    /** Human name of the payer/payee as it appeared in the SMS. */
    val counterpartyName: String? = null,

    /** Phone number of the payer/payee, normalised where possible. */
    val counterpartyNumber: String? = null,

    /** Provider transaction/reference id. */
    val reference: String? = null,

    /** Wallet/account balance after the transaction, if present. */
    val balanceAfter: Double? = null,

    val timestamp: Long,

    /** Original SMS text kept for audit / re-parsing. */
    val rawBody: String
)

enum class TxnDirection { RECEIVED, SENT }
