package com.greenleaf.paygo.sms

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

/** Thin wrapper around [SmsManager] that transparently handles long messages. */
class SmsSender(private val context: Context) {

    fun send(toNumber: String, text: String): Result<Unit> = runCatching {
        val manager = smsManager()
        val parts = manager.divideMessage(text)
        if (parts.size <= 1) {
            manager.sendTextMessage(toNumber, null, text, null, null)
        } else {
            manager.sendMultipartTextMessage(toNumber, null, parts, null, null)
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
}
