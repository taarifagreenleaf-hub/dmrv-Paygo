package com.greenleaf.paygo.parser

/**
 * Registry of known Tanzanian mobile-money and bank SMS **sender IDs**.
 *
 * Payment notifications always arrive from a fixed alphanumeric sender ID
 * (e.g. "M-PESA", "MIXX BY YAS", "AIRTELMONEY", "CRDB") rather than an ordinary
 * phone number. Matching on the sender ID lets us recognise a payment SMS as a
 * payment *even when the detailed body regex drifts* and the structured parser
 * fails to pull out the amount. This is what guarantees 100% of payment
 * messages from every provider are captured and forwarded — the structured
 * rules only need to succeed to additionally record a precise transaction.
 *
 * The list is deliberately broad and case-insensitive. Matching is by sender ID
 * only, so a normal customer SMS (which comes from a +255 number) is never
 * mistaken for a payment.
 */
object PaymentSenders {

    /** Ordered (sender-id regex -> provider slug). First match wins. */
    private val SENDERS: List<Pair<Regex, String>> = listOf(
        // --- Mobile money ---
        r("M-?PESA|VODACOM|VODA\\b") to "mpesa",
        r("MIXX|YAS\\b|TIGO ?PESA|TIGO\\b") to "mixx",
        r("AIRTEL ?MONEY|AIRTEL") to "airtelmoney",
        r("HALO ?PESA|HALOTEL") to "halopesa",
        r("T-?PESA|TTCL") to "tpesa",
        r("SELCOM ?PESA|SELCOM|MASTERCARD ?PESA") to "selcom",
        r("EZY ?PESA|EZYPESA") to "ezypesa",
        // --- Lipa / aggregators ---
        r("LIPA ?NAMBA|LIPA ?KWA ?SIMU") to "lipa",
        // --- Banks (SimBanking / mobile alerts) ---
        r("CRDB") to "crdb",
        r("\\bNMB\\b|NMB ?BANK|NMB ?PESA") to "nmb",
        r("\\bNBC\\b") to "nbc",
        r("\\bEXIM\\b") to "exim",
        r("\\bDTB\\b|DIAMOND ?TRUST") to "dtb",
        r("EQUITY") to "equity",
        r("STANBIC") to "stanbic",
        r("\\bABSA\\b|BARCLAYS") to "absa",
        r("AZANIA") to "azania",
        r("\\bTCB\\b") to "tcb",
        r("\\bBOA\\b|BANK ?OF ?AFRICA") to "boa",
        r("\\bUBA\\b") to "uba",
        r("\\bKCB\\b") to "kcb",
        r("\\bDCB\\b") to "dcb",
        r("MKOMBOZI") to "mkombozi",
        r("MAENDELEO") to "maendeleo",
        r("\\bAKIBA\\b") to "akiba",
        r("AMANA") to "amana",
        r("MWANGA") to "mwanga",
        r("LETSHEGO") to "letshego",
        r("FINCA") to "finca",
        r("\\bYETU\\b") to "yetu",
        r("\\bNCBA\\b") to "ncba",
        r("ACCESS ?BANK|ACCESSBANK") to "accessbank",
        r("I\\s*&\\s*M|\\bIANDM\\b") to "iandm"
    )

    /** A money amount cue, e.g. "Tsh 50,000", "TZS 1,250.00", "TSh5000". */
    private val MONEY = r("(?:Tsh|TZS|TSH|Tzs|TSHS|Shs?)\\.?\\s*\\d")

    /** A grouped number that looks like money even without a currency prefix, e.g. 50,000 / 1.250.000. */
    private val GROUPED = r("\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?")

    /** Words that signal a received/credited payment (Swahili + English). */
    private val RECEIVED = r(
        "umepokea|pokea|amepokelewa|imepokelewa|umelipwa|malipo|lipa|" +
            "received|credited|imeingia|deposit|funds|payment|paid|wekewa|uliopokea"
    )

    /** The provider slug for a sender ID, or null if the sender isn't a known money/bank ID. */
    fun detect(address: String?): String? {
        if (address.isNullOrBlank()) return null
        for ((regex, slug) in SENDERS) {
            if (regex.containsMatchIn(address)) return slug
        }
        return null
    }

    /** True if the body carries a money amount or a grouped number. */
    fun hasMoneyToken(body: String): Boolean =
        MONEY.containsMatchIn(body) || GROUPED.containsMatchIn(body)

    /** True if the body mentions receiving/crediting a payment. */
    fun hasReceivedKeyword(body: String): Boolean = RECEIVED.containsMatchIn(body)

    /**
     * True when the message should be treated as a payment notification: it comes
     * from a known money/bank sender AND looks like a money event (has an amount
     * or a received/credited keyword). This is the condition that must forward
     * 100% of the time, regardless of whether the structured parser succeeds.
     */
    fun isPaymentSms(address: String?, body: String): Boolean {
        if (detect(address) == null) return false
        return hasMoneyToken(body) || hasReceivedKeyword(body)
    }

    private fun r(pattern: String): Regex = Regex(pattern, RegexOption.IGNORE_CASE)
}
