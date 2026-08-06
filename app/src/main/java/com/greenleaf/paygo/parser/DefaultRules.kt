package com.greenleaf.paygo.parser

import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseTrigger
import com.greenleaf.paygo.data.db.entity.TxnDirection

/**
 * Ships a starter set of "training" rules for the common Tanzanian mobile-money
 * and bank senders. These are intentionally editable: real SMS formats drift, so
 * the user is expected to tune the regexes from the Training screen. Amounts may
 * be written as "Tsh" or "TZS", with thousands separators and optional decimals.
 */
object DefaultRules {

    // Reusable fragments -----------------------------------------------------
    private const val AMOUNT = """(?:Tsh|TZS|TSH)\.?\s*([\d.,]+)"""
    private const val PHONE = """(\+?255\d{9}|0\d{9})"""
    private const val BALANCE = """(?:balance|Salio)[^\d]{0,20}(?:Tsh|TZS|TSH)\.?\s*([\d.,]+)"""

    fun parsingRules(): List<ParsingRuleEntity> = listOf(
        // --- M-Pesa (Vodacom) ---
        ParsingRuleEntity(
            name = "M-Pesa received",
            provider = "mpesa",
            isBuiltIn = true,
            priority = 100,
            senderPattern = "M-?PESA",
            bodyPattern = "(?i)(received|umepokea|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:received|umepokea)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)(?:from|kutoka(?: kwa)?)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=$PHONE|on|tarehe|\\d{1,2}/)",
            numberRegex = PHONE,
            referenceRegex = "(?i)^\\s*([A-Z0-9]{8,12})\\b",
            balanceRegex = BALANCE
        ),
        // --- Mixx by Yas (formerly Tigo Pesa) ---
        ParsingRuleEntity(
            name = "Mixx by Yas received",
            provider = "mixx",
            isBuiltIn = true,
            priority = 90,
            senderPattern = "(MIXX|YAS|Tigo\\s?Pesa)",
            bodyPattern = "(?i)(umepokea|received|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:umepokea|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)kutoka(?: kwa)?\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE)",
            numberRegex = PHONE,
            referenceRegex = "(?i)(?:Kumbukumbu|TID|Muamala)[:\\s]+([A-Z0-9]{6,15})",
            balanceRegex = BALANCE
        ),
        // --- HaloPesa (Halotel) ---
        ParsingRuleEntity(
            name = "HaloPesa received",
            provider = "halopesa",
            isBuiltIn = true,
            priority = 80,
            senderPattern = "HaloPesa",
            bodyPattern = "(?i)(umepokea|received|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:umepokea|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)kutoka(?: kwa)?\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE)",
            numberRegex = PHONE,
            referenceRegex = "(?i)(?:Kumbukumbu|TID)[:\\s]+([A-Z0-9]{6,15})",
            balanceRegex = BALANCE
        ),
        // --- T-Pesa (TTCL) ---
        ParsingRuleEntity(
            name = "T-Pesa received",
            provider = "tpesa",
            isBuiltIn = true,
            priority = 70,
            senderPattern = "T-?PESA",
            bodyPattern = "(?i)(umepokea|received|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:umepokea|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)kutoka(?: kwa)?\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE)",
            numberRegex = PHONE,
            balanceRegex = BALANCE
        ),
        // --- Airtel Money ---
        ParsingRuleEntity(
            name = "Airtel Money received",
            provider = "airtelmoney",
            isBuiltIn = true,
            priority = 60,
            senderPattern = "Airtel\\s?Money",
            bodyPattern = "(?i)(received|umepokea|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:received|umepokea)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)(?:from|kutoka(?: kwa)?)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE)",
            numberRegex = PHONE,
            balanceRegex = BALANCE
        ),
        // --- Selcom (Selcom Pesa) ---
        ParsingRuleEntity(
            name = "Selcom received",
            provider = "selcom",
            isBuiltIn = true,
            priority = 50,
            senderPattern = "SELCOM",
            bodyPattern = "(?i)(umepokea|received|pokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:umepokea|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)(?:from|kutoka(?: kwa)?)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE)",
            numberRegex = PHONE,
            balanceRegex = BALANCE
        ),
        // --- CRDB Bank (SimBanking) ---
        ParsingRuleEntity(
            name = "CRDB credit",
            provider = "crdb",
            isBuiltIn = true,
            priority = 40,
            senderPattern = "CRDB",
            bodyPattern = "(?i)(credited|imeingia|received|umepokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:credited|imeingia|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)(?:from|kutoka(?: kwa)?)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE|on|tarehe)",
            numberRegex = PHONE,
            referenceRegex = "(?i)(?:Ref|Reference|Kumbukumbu)[:\\s]+([A-Z0-9]{6,20})",
            balanceRegex = BALANCE
        ),
        // --- NMB Bank (NMB Mkononi) ---
        ParsingRuleEntity(
            name = "NMB credit",
            provider = "nmb",
            isBuiltIn = true,
            priority = 30,
            senderPattern = "NMB",
            bodyPattern = "(?i)(credited|imeingia|received|umepokea)",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)(?:credited|imeingia|received)[^\\d]{0,15}$AMOUNT",
            nameRegex = "(?i)(?:from|kutoka(?: kwa)?)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|$PHONE|on|tarehe)",
            numberRegex = PHONE,
            referenceRegex = "(?i)(?:Ref|Reference|Kumbukumbu)[:\\s]+([A-Z0-9]{6,20})",
            balanceRegex = BALANCE
        )
    )

    fun responseRules(): List<ResponseRuleEntity> = listOf(
        ResponseRuleEntity(
            name = "Thank you for payment",
            triggerType = ResponseTrigger.PAYMENT.name,
            priority = 100,
            replyViaSms = true,
            replyViaWhatsApp = true,
            template = "Asante {name}! Tumepokea malipo ya {currency} {amount} " +
                "kupitia {provider}. Kumbukumbu: {reference}. Karibu tena."
        ),
        ResponseRuleEntity(
            name = "Auto-ack normal message",
            triggerType = ResponseTrigger.NORMAL.name,
            enabled = false,
            priority = 10,
            replyViaSms = true,
            template = "Asante kwa ujumbe wako. Tutakujibu hivi karibuni."
        )
    )
}
