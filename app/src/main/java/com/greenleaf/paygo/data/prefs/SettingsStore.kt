package com.greenleaf.paygo.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "paygo_settings")

/** Immutable snapshot of user configuration. */
data class PaygoSettings(
    val masterEnabled: Boolean = false,
    val autoReplyEnabled: Boolean = true,

    val forwardEnabled: Boolean = false,
    /** Comma-separated phone numbers to forward incoming messages to via SMS. */
    val forwardSmsNumbers: String = "",
    /** Single number (E.164) to forward to via WhatsApp. */
    val forwardWhatsAppNumber: String = "",
    val forwardOnlyPayments: Boolean = true,

    /** How WhatsApp messages are delivered: INTENT, ACCESSIBILITY or CLOUD_API.
     *  Defaults to ACCESSIBILITY so WhatsApp is hands-free on-device (SMS is
     *  already sent automatically through the mobile operator). */
    val whatsAppMode: String = WhatsAppMode.ACCESSIBILITY.name,
    val cloudApiToken: String = "",
    val cloudApiPhoneNumberId: String = ""
) {
    val forwardSmsList: List<String>
        get() = forwardSmsNumbers.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() }
}

enum class WhatsAppMode { INTENT, ACCESSIBILITY, CLOUD_API }

class SettingsStore(private val context: Context) {

    val settings: Flow<PaygoSettings> = context.dataStore.data.map { p ->
        PaygoSettings(
            masterEnabled = p[MASTER] ?: false,
            autoReplyEnabled = p[AUTO_REPLY] ?: true,
            forwardEnabled = p[FORWARD] ?: false,
            forwardSmsNumbers = p[FWD_SMS] ?: "",
            forwardWhatsAppNumber = p[FWD_WA] ?: "",
            forwardOnlyPayments = p[FWD_ONLY_PAY] ?: true,
            whatsAppMode = p[WA_MODE] ?: WhatsAppMode.ACCESSIBILITY.name,
            cloudApiToken = p[WA_TOKEN] ?: "",
            cloudApiPhoneNumberId = p[WA_PHONE_ID] ?: ""
        )
    }

    suspend fun update(transform: (PaygoSettings) -> PaygoSettings) {
        context.dataStore.edit { p ->
            val current = PaygoSettings(
                masterEnabled = p[MASTER] ?: false,
                autoReplyEnabled = p[AUTO_REPLY] ?: true,
                forwardEnabled = p[FORWARD] ?: false,
                forwardSmsNumbers = p[FWD_SMS] ?: "",
                forwardWhatsAppNumber = p[FWD_WA] ?: "",
                forwardOnlyPayments = p[FWD_ONLY_PAY] ?: true,
                whatsAppMode = p[WA_MODE] ?: WhatsAppMode.ACCESSIBILITY.name,
                cloudApiToken = p[WA_TOKEN] ?: "",
                cloudApiPhoneNumberId = p[WA_PHONE_ID] ?: ""
            )
            val next = transform(current)
            p[MASTER] = next.masterEnabled
            p[AUTO_REPLY] = next.autoReplyEnabled
            p[FORWARD] = next.forwardEnabled
            p[FWD_SMS] = next.forwardSmsNumbers
            p[FWD_WA] = next.forwardWhatsAppNumber
            p[FWD_ONLY_PAY] = next.forwardOnlyPayments
            p[WA_MODE] = next.whatsAppMode
            p[WA_TOKEN] = next.cloudApiToken
            p[WA_PHONE_ID] = next.cloudApiPhoneNumberId
        }
    }

    private companion object {
        val MASTER = booleanPreferencesKey("master_enabled")
        val AUTO_REPLY = booleanPreferencesKey("auto_reply")
        val FORWARD = booleanPreferencesKey("forward_enabled")
        val FWD_SMS = stringPreferencesKey("forward_sms_numbers")
        val FWD_WA = stringPreferencesKey("forward_whatsapp_number")
        val FWD_ONLY_PAY = booleanPreferencesKey("forward_only_payments")
        val WA_MODE = stringPreferencesKey("whatsapp_mode")
        val WA_TOKEN = stringPreferencesKey("cloud_api_token")
        val WA_PHONE_ID = stringPreferencesKey("cloud_api_phone_id")
    }
}
