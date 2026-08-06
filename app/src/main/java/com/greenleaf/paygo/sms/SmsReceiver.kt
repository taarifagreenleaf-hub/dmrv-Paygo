package com.greenleaf.paygo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage

/**
 * Listens for incoming SMS and hands each (sender, full body) pair to
 * [SmsProcessingService]. Multipart messages are concatenated per sender so the
 * parser sees the complete text.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages: Array<SmsMessage> =
            Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages.first().displayOriginatingAddress ?: messages.first().originatingAddress ?: "unknown"
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val timestamp = messages.first().timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
        val simId = intent.getIntExtra("subscription", -1)

        val serviceIntent = Intent(context, SmsProcessingService::class.java).apply {
            putExtra(SmsProcessingService.EXTRA_ADDRESS, sender)
            putExtra(SmsProcessingService.EXTRA_BODY, body)
            putExtra(SmsProcessingService.EXTRA_TIMESTAMP, timestamp)
            putExtra(SmsProcessingService.EXTRA_SIM_ID, simId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
