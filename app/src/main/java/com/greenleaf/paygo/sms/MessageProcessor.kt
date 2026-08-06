package com.greenleaf.paygo.sms

import com.greenleaf.paygo.data.db.entity.ContactGroupEntity
import com.greenleaf.paygo.data.db.entity.CustomerEntity
import com.greenleaf.paygo.data.db.entity.EventLogEntity
import com.greenleaf.paygo.data.db.entity.MessageCategory
import com.greenleaf.paygo.data.db.entity.MessageEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseTrigger
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import com.greenleaf.paygo.data.prefs.PaygoSettings
import com.greenleaf.paygo.data.prefs.SettingsStore
import com.greenleaf.paygo.data.repo.PaygoRepository
import com.greenleaf.paygo.parser.InfoExtractor
import com.greenleaf.paygo.parser.ParseResult
import com.greenleaf.paygo.parser.PaymentParser
import com.greenleaf.paygo.util.CustomerId
import com.greenleaf.paygo.util.GroupKey
import com.greenleaf.paygo.util.TemplateEngine
import com.greenleaf.paygo.whatsapp.WhatsAppSender
import kotlinx.coroutines.flow.first
import org.json.JSONObject

/**
 * The heart of the app. Takes a raw incoming SMS and:
 *  1. parses it for a payment,
 *  2. stores the message (+ transaction) and updates the payee group,
 *  3. runs auto-reply response rules (SMS + WhatsApp),
 *  4. forwards it to the configured numbers/WhatsApp.
 */
class MessageProcessor(
    private val repo: PaygoRepository,
    private val settingsStore: SettingsStore,
    private val parser: PaymentParser,
    private val infoExtractor: InfoExtractor,
    private val smsSender: SmsSender,
    private val whatsAppSender: WhatsAppSender
) {

    suspend fun process(address: String, body: String, timestamp: Long, simId: Int) {
        val settings = settingsStore.settings.first()
        val rules = repo.enabledParsingRules()
        val parse: ParseResult? = parser.parse(address, body, rules)

        val category = if (parse?.isPayment == true) MessageCategory.PAYMENT else MessageCategory.NORMAL
        val groupKey = if (parse?.isPayment == true) {
            GroupKey.forCounterparty(parse.counterpartyNumber, parse.counterpartyName, address)
        } else {
            GroupKey.forSender(address)
        }

        // For free-form (non-payment) messages, pull out key customer info.
        val infoMap = if (parse?.isPayment == true) emptyMap() else
            infoExtractor.extract(body, repo.enabledInfoRules())
        val extractedInfo = if (infoMap.isEmpty()) null else JSONObject(infoMap as Map<*, *>).toString()

        // Resolve the customer this message belongs to.
        val customer = resolveCustomer(parse, address, body, infoMap, settings, timestamp)

        val message = MessageEntity(
            address = address,
            body = body,
            timestamp = timestamp,
            simSubscriptionId = simId,
            category = category.name,
            provider = parse?.provider,
            groupKey = groupKey,
            extractedInfo = extractedInfo,
            customerId = customer?.custId
        )
        val messageId = repo.insertMessage(message)
        val stored = message.copy(id = messageId)

        var transaction: TransactionEntity? = null
        if (parse?.isPayment == true) {
            transaction = TransactionEntity(
                messageId = messageId,
                provider = parse.provider,
                direction = parse.direction,
                amount = parse.amount!!,
                counterpartyName = parse.counterpartyName,
                counterpartyNumber = parse.counterpartyNumber,
                customerId = customer?.custId,
                reference = parse.reference,
                balanceAfter = parse.balanceAfter,
                timestamp = timestamp,
                rawBody = body
            )
            repo.insertTransaction(transaction)
        }

        // Keep the customer record up to date (totals from payments, details from replies).
        if (customer != null) {
            updateCustomer(customer, parse, infoMap, timestamp)
        }

        updateGroup(groupKey, stored, transaction)

        if (settings.masterEnabled) {
            if (settings.autoReplyEnabled) autoReply(stored, transaction, settings)
            if (settings.forwardEnabled) forward(stored, transaction, settings)
        }
    }

    /**
     * Decides which customer a message belongs to:
     *  - a payment creates (or reuses) a customer keyed by the payer's number;
     *  - a normal message is linked either by an sms-cust-id it quotes, or by the
     *    sender's number matching a known customer.
     */
    private suspend fun resolveCustomer(
        parse: ParseResult?,
        address: String,
        body: String,
        info: Map<String, String>,
        settings: PaygoSettings,
        now: Long
    ): CustomerEntity? {
        if (!settings.trackCustomers) return null

        if (parse?.isPayment == true) {
            return repo.findOrCreateCustomer(
                number = parse.counterpartyNumber,
                prefix = settings.customerIdPrefix,
                name = parse.counterpartyName,
                now = now
            )
        }

        // Normal message: prefer an id the customer quoted, then their number.
        CustomerId.detectSeq(body, settings.customerIdPrefix)?.let { seq ->
            repo.getCustomerBySeq(seq)?.let { return it }
        }
        val fromNumber = normalizeNumber(address) ?: info["phone"]?.let { normalizeNumber(it) }
        if (fromNumber != null) {
            repo.allCustomers().firstOrNull { it.phoneNumber == fromNumber }?.let { return it }
        }
        return null
    }

    /** Merges payment totals and reply-supplied details into the customer record. */
    private suspend fun updateCustomer(
        customer: CustomerEntity,
        parse: ParseResult?,
        info: Map<String, String>,
        now: Long
    ) {
        val isPayment = parse?.isPayment == true
        val updated = customer.copy(
            name = customer.name ?: parse?.counterpartyName?.ifBlank { null } ?: info["name"],
            location = info["location"] ?: customer.location,
            productType = info["product_type"] ?: info["system_size"] ?: customer.productType,
            phoneNumber = customer.phoneNumber ?: parse?.counterpartyNumber,
            totalPaid = customer.totalPaid + if (isPayment) (parse?.amount ?: 0.0) else 0.0,
            lastAmount = if (isPayment) parse?.amount else customer.lastAmount,
            paymentCount = customer.paymentCount + if (isPayment) 1 else 0,
            updatedAt = now
        )
        repo.updateCustomer(updated)
    }

    private fun normalizeNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() || it == '+' }
        if (digits.length < 9) return null
        return when {
            digits.startsWith("+") -> digits
            digits.startsWith("255") -> "+$digits"
            digits.startsWith("0") && digits.length == 10 -> "+255${digits.substring(1)}"
            else -> digits
        }
    }

    private suspend fun updateGroup(groupKey: String, message: MessageEntity, txn: TransactionEntity?) {
        val existing = repo.getGroup(groupKey)
        val displayName = txn?.counterpartyName?.takeIf { it.isNotBlank() }
            ?: existing?.displayName
            ?: message.address
        repo.upsertGroup(
            ContactGroupEntity(
                groupKey = groupKey,
                displayName = displayName,
                lastMessageAt = message.timestamp,
                messageCount = (existing?.messageCount ?: 0) + 1,
                totalReceived = (existing?.totalReceived ?: 0.0) + (txn?.amount ?: 0.0),
                note = existing?.note
            )
        )
    }

    private suspend fun autoReply(message: MessageEntity, txn: TransactionEntity?, settings: PaygoSettings) {
        val rule = matchResponseRule(message, txn) ?: return
        val text = TemplateEngine.render(rule.template, message, txn)
        val replyTarget = txn?.counterpartyNumber ?: message.address
        if (!isSendableNumber(replyTarget)) return

        var repliedSms = false
        var repliedWa = false

        if (rule.replyViaSms) {
            val result = smsSender.send(replyTarget, text)
            repliedSms = result.isSuccess
            log(message.id, "REPLY_SMS", replyTarget, text, result)
        }
        if (rule.replyViaWhatsApp) {
            val result = whatsAppSender.send(replyTarget, text, settings)
            repliedWa = result.isSuccess
            log(message.id, "REPLY_WHATSAPP", replyTarget, text, result)
        }
        repo.updateMessage(
            message.copy(handled = true, repliedSms = repliedSms, repliedWhatsApp = repliedWa)
        )
    }

    private suspend fun matchResponseRule(message: MessageEntity, txn: TransactionEntity?): ResponseRuleEntity? {
        val rules = repo.enabledResponseRules()
        val isPayment = txn != null
        return rules.firstOrNull { rule ->
            when (ResponseTrigger.valueOf(rule.triggerType)) {
                ResponseTrigger.PAYMENT -> isPayment &&
                    (rule.providerFilter == null || rule.providerFilter == txn?.provider) &&
                    (rule.minAmount == null || (txn?.amount ?: 0.0) >= rule.minAmount) &&
                    (rule.maxAmount == null || (txn?.amount ?: 0.0) <= rule.maxAmount)
                ResponseTrigger.NORMAL -> !isPayment
                ResponseTrigger.KEYWORD -> {
                    val kws = rule.keywords?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }
                    kws != null && kws.any { message.body.lowercase().contains(it) }
                }
            }
        }
    }

    private suspend fun forward(message: MessageEntity, txn: TransactionEntity?, settings: PaygoSettings) {
        if (settings.forwardOnlyPayments && txn == null) return
        val header = buildForwardHeader(message, txn)
        val fullText = "$header\n${message.body}"

        var forwardedAny = false
        for (number in settings.forwardSmsList) {
            if (!isSendableNumber(number)) continue
            val result = smsSender.send(number, fullText)
            forwardedAny = forwardedAny || result.isSuccess
            log(message.id, "FORWARD_SMS", number, fullText, result)
        }
        if (settings.forwardWhatsAppNumber.isNotBlank()) {
            val result = whatsAppSender.send(settings.forwardWhatsAppNumber, fullText, settings)
            forwardedAny = forwardedAny || result.isSuccess
            log(message.id, "FORWARD_WHATSAPP", settings.forwardWhatsAppNumber, fullText, result)
        }
        if (forwardedAny) repo.updateMessage(message.copy(forwarded = true))
    }

    private fun buildForwardHeader(message: MessageEntity, txn: TransactionEntity?): String =
        if (txn != null) {
            "[Paygo] Payment ${txn.currency} ${TemplateEngine.formatAmount(txn.amount)} " +
                "via ${txn.provider} from ${txn.counterpartyName ?: "?"} " +
                "(${txn.counterpartyNumber ?: message.address})"
        } else {
            "[Paygo] Message from ${message.address}"
        }

    private fun isSendableNumber(value: String): Boolean {
        val digits = value.filter { it.isDigit() }
        return digits.length >= 6 // skip alphanumeric sender ids like "M-PESA"
    }

    private suspend fun log(messageId: Long, action: String, target: String?, detail: String, result: Result<Unit>) {
        repo.logEvent(
            EventLogEntity(
                messageId = messageId,
                action = if (result.isSuccess) action else "ERROR",
                target = target,
                detail = if (result.isSuccess) detail
                else "$action failed: ${result.exceptionOrNull()?.message}",
                success = result.isSuccess,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
