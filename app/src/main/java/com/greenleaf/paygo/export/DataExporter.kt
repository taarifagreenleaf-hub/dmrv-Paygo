package com.greenleaf.paygo.export

import android.content.Context
import com.greenleaf.paygo.data.db.entity.MessageEntity
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import com.greenleaf.paygo.data.repo.PaygoRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Produces downloadable/shareable exports of everything the app has captured:
 *  - transactions.csv  — spreadsheet-friendly payment ledger
 *  - messages.csv      — every SMS received
 *  - paygo-backup.json — full backup (messages + transactions + rules) for
 *                        restore or transfer to another device.
 */
class DataExporter(private val context: Context, private val repo: PaygoRepository) {

    private val stamp: String get() =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private fun outDir(): File =
        File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

    suspend fun exportTransactionsCsv(): File {
        val rows = repo.allTransactions()
        val sb = StringBuilder()
        sb.appendLine("id,timestamp,provider,direction,amount,currency,name,number,reference,balanceAfter")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (t in rows) {
            sb.appendLine(
                listOf(
                    t.id,
                    df.format(Date(t.timestamp)),
                    t.provider,
                    t.direction,
                    t.amount,
                    t.currency,
                    csv(t.counterpartyName),
                    csv(t.counterpartyNumber),
                    csv(t.reference),
                    t.balanceAfter ?: ""
                ).joinToString(",")
            )
        }
        return write("transactions-$stamp.csv", sb.toString())
    }

    suspend fun exportMessagesCsv(): File {
        val rows = repo.allMessages()
        val sb = StringBuilder()
        sb.appendLine("id,timestamp,address,category,provider,groupKey,extractedInfo,body")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (m in rows) {
            sb.appendLine(
                listOf(
                    m.id,
                    df.format(Date(m.timestamp)),
                    csv(m.address),
                    m.category,
                    csv(m.provider),
                    csv(m.groupKey),
                    csv(m.extractedInfo),
                    csv(m.body)
                ).joinToString(",")
            )
        }
        return write("messages-$stamp.csv", sb.toString())
    }

    suspend fun exportBackupJson(): File {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)
        root.put("messages", JSONArray(repo.allMessages().map { it.toJson() }))
        root.put("transactions", JSONArray(repo.allTransactions().map { it.toJson() }))
        return write("paygo-backup-$stamp.json", root.toString(2))
    }

    private fun write(name: String, content: String): File {
        val file = File(outDir(), name)
        file.writeText(content)
        return file
    }

    private fun csv(value: String?): String {
        if (value == null) return ""
        val escaped = value.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
        return "\"$escaped\""
    }

    private fun MessageEntity.toJson() = JSONObject().apply {
        put("id", id); put("address", address); put("body", body); put("timestamp", timestamp)
        put("category", category); put("provider", provider); put("groupKey", groupKey)
        put("extractedInfo", extractedInfo)
    }

    private fun TransactionEntity.toJson() = JSONObject().apply {
        put("id", id); put("messageId", messageId); put("provider", provider)
        put("direction", direction); put("amount", amount); put("currency", currency)
        put("name", counterpartyName); put("number", counterpartyNumber)
        put("reference", reference); put("balanceAfter", balanceAfter); put("timestamp", timestamp)
    }
}
