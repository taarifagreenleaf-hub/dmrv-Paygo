package com.greenleaf.paygo.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.greenleaf.paygo.data.prefs.PaygoSettings
import com.greenleaf.paygo.data.prefs.WhatsAppMode
import java.net.URLEncoder

/**
 * Delivers a WhatsApp message using whichever strategy the user configured:
 *
 *  - CLOUD_API   → official Meta WhatsApp Cloud API (fully automatic, recommended).
 *  - ACCESSIBILITY → opens the pre-filled chat and lets [WhatsAppAccessibilityService]
 *                    tap "send" automatically.
 *  - INTENT      → opens the pre-filled chat; the user taps send manually.
 */
class WhatsAppSender(private val context: Context) {

    fun send(toNumber: String, text: String, settings: PaygoSettings): Result<Unit> {
        return when (WhatsAppMode.valueOf(settings.whatsAppMode)) {
            WhatsAppMode.CLOUD_API -> {
                if (settings.cloudApiToken.isBlank() || settings.cloudApiPhoneNumberId.isBlank()) {
                    Result.failure(IllegalStateException("WhatsApp Cloud API not configured"))
                } else {
                    WhatsAppCloudApi(settings.cloudApiToken, settings.cloudApiPhoneNumberId)
                        .sendText(toNumber, text)
                }
            }
            WhatsAppMode.ACCESSIBILITY -> openChat(toNumber, text, autoSend = true)
            WhatsAppMode.INTENT -> openChat(toNumber, text, autoSend = false)
        }
    }

    private fun openChat(toNumber: String, text: String, autoSend: Boolean): Result<Unit> = runCatching {
        val phone = toNumber.filter { it.isDigit() }
        val encoded = URLEncoder.encode(text, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (autoSend) WhatsAppAutoSendState.arm()
        context.startActivity(intent)
    }.recoverCatching {
        // Fall back to WhatsApp Business if consumer app isn't installed.
        val phone = toNumber.filter { it.isDigit() }
        val encoded = URLEncoder.encode(text, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (autoSend) WhatsAppAutoSendState.arm()
        context.startActivity(intent)
    }
}
