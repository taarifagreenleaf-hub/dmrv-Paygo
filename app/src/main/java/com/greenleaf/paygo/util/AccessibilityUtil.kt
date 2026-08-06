package com.greenleaf.paygo.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.greenleaf.paygo.whatsapp.WhatsAppAccessibilityService

/** Helpers for the WhatsApp hands-free auto-send accessibility service. */
object AccessibilityUtil {

    /** True when the user has switched on Paygo's accessibility service. */
    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, WhatsAppAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component != null && component == expected) return true
        }
        return false
    }

    /** Opens Android's Accessibility settings so the user can enable the service. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
