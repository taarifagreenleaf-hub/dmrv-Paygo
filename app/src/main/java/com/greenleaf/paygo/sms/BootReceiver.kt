package com.greenleaf.paygo.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greenleaf.paygo.di.ServiceLocator

/**
 * Ensures the dependency graph is initialised after a reboot so the manifest
 * SMS receiver can process the first message without the UI being opened first.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ServiceLocator.init(context)
        }
    }
}
