package com.greenleaf.paygo.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.greenleaf.paygo.R
import com.greenleaf.paygo.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Short-lived foreground service that runs one message through
 * [MessageProcessor]. Running in the foreground lets us reliably send SMS,
 * write to the database and (optionally) open WhatsApp even when the app UI is
 * not in the foreground. The service stops itself once the queue drains.
 */
class SmsProcessingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlight = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopIfIdle()
            return START_NOT_STICKY
        }
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: "unknown"
        val body = intent.getStringExtra(EXTRA_BODY) ?: ""
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        val simId = intent.getIntExtra(EXTRA_SIM_ID, -1)

        inFlight.incrementAndGet()
        scope.launch {
            try {
                ServiceLocator.messageProcessor.process(address, body, timestamp, simId)
            } finally {
                inFlight.decrementAndGet()
                stopIfIdle()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopIfIdle() {
        if (inFlight.get() <= 0) stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_BODY = "body"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_SIM_ID = "sim_id"

        private const val CHANNEL_ID = "paygo_processing"
        private const val NOTIFICATION_ID = 4201
    }
}
