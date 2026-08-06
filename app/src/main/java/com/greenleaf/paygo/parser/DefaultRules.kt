package com.greenleaf.paygo.parser

import com.greenleaf.paygo.data.db.entity.InfoRuleEntity
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
        // --- Selcom "Lipa Kwa Simu" (vending) — highest priority ---
        // Handles both observed formats:
        //   "Umepokea malipo TSh 100,000 kutoka kwa Airtel Money; 255... - NAME
        //    Kumbukumbu No.: 267... Salio lako jipya ni TSh 3,348,479."
        //   "Umepokea Malipo ya TSh 80,000 kwenye Lipa namba 459806551 kutoka kwa
        //    255... - NAME. Kumbukumbu No.: 267... Salio lako jipya ni TSh ..."
        ParsingRuleEntity(
            name = "Lipa Kwa Simu (Selcom)",
            provider = "selcom",
            isBuiltIn = true,
            priority = 120,
            senderPattern = ".*",
            bodyPattern = "(?i)Lipa Kwa Simu",
            direction = TxnDirection.RECEIVED.name,
            amountRegex = "(?i)Umepokea\\s+Malipo(?:\\s+ya)?\\s+TSh\\s*([\\d.,]+)",
            // Funding wallet when named (e.g. "Airtel Money"); blank for direct
            // phone-number payments, in which case provider falls back to "selcom".
            providerRegex = "(?i)kutoka kwa\\s+([A-Za-z][A-Za-z .\\-]+?)\\s*;",
            nameRegex = "-\\s+([A-Za-z][A-Za-z'. ]+?)\\s*\\.?\\s*Kumbukumbu",
            numberRegex = "(255\\d{9})",
            referenceRegex = "(?i)Kumbukumbu No\\.?:?\\s*([0-9]{10,16})",
            balanceRegex = "(?i)Salio lako jipya ni TSh\\s*([\\d.,]+)"
        ),
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

    /**
     * Starter extractors for free-form customer messages. Customers write in a
     * mix of Swahili and English and often only include some fields, so these are
     * keyword-anchored and meant to be tuned from the Capture screen.
     */
    fun infoRules(): List<InfoRuleEntity> = listOf(
        InfoRuleEntity(
            name = "Name",
            fieldKey = "name",
            isBuiltIn = true,
            priority = 100,
            regex = "(?i)(?:jina(?:\\s+langu)?|name)\\s*[:\\-]?\\s*([A-Za-z][A-Za-z' ]{2,40})"
        ),
        InfoRuleEntity(
            name = "Location",
            fieldKey = "location",
            isBuiltIn = true,
            priority = 90,
            regex = "(?i)(?:mahali|eneo|location|kata|kijiji|mtaa|nipo|niko|from)\\s*[:\\-]?\\s*([A-Za-z][A-Za-z' ]{2,40})"
        ),
        InfoRuleEntity(
            name = "Product type",
            fieldKey = "product_type",
            isBuiltIn = true,
            priority = 85,
            regex = "(?i)(?:aina(?:\\s+ya\\s+bidhaa)?|bidhaa|product|mfumo)\\s*[:\\-]?\\s*([A-Za-z0-9][A-Za-z0-9' ]{1,40})"
        ),
        InfoRuleEntity(
            name = "Amount",
            fieldKey = "amount",
            isBuiltIn = true,
            priority = 80,
            regex = "(?i)(?:nimelipa|nalipa|kiasi|amount|tsh|tzs)\\s*[:\\-]?\\s*([\\d.,]{3,})"
        ),
        InfoRuleEntity(
            name = "System size",
            fieldKey = "system_size",
            isBuiltIn = true,
            priority = 70,
            regex = "(?i)(\\d{1,4}\\s?(?:wp|watts|watt|w|kw)\\b|SHS\\s?\\d+|system\\s?\\w+)"
        ),
        InfoRuleEntity(
            name = "Phone",
            fieldKey = "phone",
            isBuiltIn = true,
            priority = 60,
            regex = "(\\+?255\\d{9}|0\\d{9})"
        )
    )

    fun responseRules(): List<ResponseRuleEntity> = listOf(
        ResponseRuleEntity(
            name = "Thank you for payment",
            triggerType = ResponseTrigger.PAYMENT.name,
            priority = 100,
            replyViaSms = true,
            replyViaWhatsApp = true,
            template = "Asante {name}! Tumepokea {currency} {amount} kupitia {provider}. " +
                "Namba yako ya mteja (Customer ID) ni {custid}. Tafadhali jibu ujumbe huu " +
                "ukiandika {custid}, jina lako, eneo/mahali, na aina ya bidhaa (product). " +
                "Kumbukumbu: {reference}."
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
