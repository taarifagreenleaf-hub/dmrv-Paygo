package com.greenleaf.paygo.util

import com.greenleaf.paygo.data.db.entity.MessageEntity
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Fills reply templates from a message + optional parsed transaction. */
object TemplateEngine {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun render(template: String, message: MessageEntity, txn: TransactionEntity?): String {
        val values = mapOf(
            "name" to (txn?.counterpartyName ?: ""),
            "amount" to (txn?.amount?.let { formatAmount(it) } ?: ""),
            "currency" to (txn?.currency ?: "TZS"),
            "provider" to (txn?.provider ?: message.provider ?: ""),
            "reference" to (txn?.reference ?: ""),
            "number" to (txn?.counterpartyNumber ?: message.address),
            "balance" to (txn?.balanceAfter?.let { formatAmount(it) } ?: ""),
            "date" to dateFmt.format(Date(message.timestamp)),
            "sender" to message.address
        )
        var out = template
        for ((key, value) in values) {
            out = out.replace("{$key}", value)
        }
        return out.trim()
    }

    fun formatAmount(value: Double): String {
        val nf = java.text.NumberFormat.getNumberInstance(Locale.US)
        nf.maximumFractionDigits = 2
        nf.minimumFractionDigits = 0
        return nf.format(value)
    }
}
