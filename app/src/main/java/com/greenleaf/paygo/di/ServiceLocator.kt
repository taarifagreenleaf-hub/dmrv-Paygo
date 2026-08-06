package com.greenleaf.paygo.di

import android.content.Context
import com.greenleaf.paygo.data.db.PaygoDatabase
import com.greenleaf.paygo.data.prefs.SettingsStore
import com.greenleaf.paygo.data.repo.PaygoRepository
import com.greenleaf.paygo.parser.InfoExtractor
import com.greenleaf.paygo.parser.PaymentParser
import com.greenleaf.paygo.sms.MessageProcessor
import com.greenleaf.paygo.sms.SmsSender
import com.greenleaf.paygo.whatsapp.WhatsAppSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Minimal manual dependency container — avoids the extra build surface of a DI
 * framework while still giving every component a single shared instance.
 */
object ServiceLocator {

    @Volatile private var appContext: Context? = null

    val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    private fun ctx(): Context =
        appContext ?: error("ServiceLocator.init() not called")

    val database: PaygoDatabase by lazy { PaygoDatabase.get(ctx()) }
    val repository: PaygoRepository by lazy { PaygoRepository(database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(ctx()) }
    val parser: PaymentParser by lazy { PaymentParser() }
    val infoExtractor: InfoExtractor by lazy { InfoExtractor() }
    val smsSender: SmsSender by lazy { SmsSender(ctx()) }
    val whatsAppSender: WhatsAppSender by lazy { WhatsAppSender(ctx()) }

    val messageProcessor: MessageProcessor by lazy {
        MessageProcessor(repository, settingsStore, parser, infoExtractor, smsSender, whatsAppSender)
    }
}
