package com.greenleaf.paygo.whatsapp

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordination point between [WhatsAppSender] (which opens a pre-filled WhatsApp
 * chat) and [WhatsAppAccessibilityService] (which taps the send button). When a
 * message is queued the accessibility service will click "send" on the next
 * WhatsApp compose screen it sees, then disarm.
 */
object WhatsAppAutoSendState {
    private val armed = AtomicBoolean(false)

    fun arm() = armed.set(true)
    fun disarm() = armed.set(false)
    fun isArmed(): Boolean = armed.get()
}
