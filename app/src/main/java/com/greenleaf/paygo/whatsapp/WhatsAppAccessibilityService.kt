package com.greenleaf.paygo.whatsapp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional helper: when [WhatsAppAutoSendState] is armed (i.e. we just opened a
 * pre-filled WhatsApp chat), this service locates the WhatsApp "send" button and
 * taps it, so replies/forwards go out without the user pressing send.
 *
 * The user must explicitly enable this in Android's Accessibility settings; it
 * only reads WhatsApp windows and performs a single click.
 */
class WhatsAppAccessibilityService : AccessibilityService() {

    private val sendButtonIds = listOf(
        "com.whatsapp:id/send",
        "com.whatsapp.w4b:id/send"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!WhatsAppAutoSendState.isArmed()) return
        val root = rootInActiveWindow ?: return
        for (id in sendButtonIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            val button = nodes.firstOrNull { it.isClickable && it.isVisibleToUser }
            if (button != null) {
                val clicked = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    WhatsAppAutoSendState.disarm()
                    return
                }
            }
        }
    }

    override fun onInterrupt() { /* no-op */ }
}
