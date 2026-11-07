package com.greenleaf.paygo.parser

import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity

/**
 * Evaluates an SMS against the user's parsing rules. The first enabled rule whose
 * [ParsingRuleEntity.senderPattern] matches the sender AND whose
 * [ParsingRuleEntity.bodyPattern] matches the body wins; its field regexes are
 * then applied to extract the payment details.
 *
 * All regexes are compiled defensively — a bad user-entered pattern is skipped
 * rather than crashing message processing.
 */
class PaymentParser {

    fun parse(address: String, body: String, rules: List<ParsingRuleEntity>): ParseResult? {
        for (rule in rules) {
            if (!rule.enabled) continue
            val senderRegex = safeRegex(rule.senderPattern) ?: continue
            val bodyRegex = safeRegex(rule.bodyPattern) ?: continue
            if (!senderRegex.containsMatchIn(address)) continue
            if (!bodyRegex.containsMatchIn(body)) continue

            val amount = AmountParser.parse(extract(body, rule.amountRegex, rule.amountGroup))
            val name = extract(body, rule.nameRegex, rule.nameGroup)?.trim()?.trim('.', ',')
            val number = normalizeNumber(extract(body, rule.numberRegex, rule.numberGroup))
            val reference = extract(body, rule.referenceRegex, rule.referenceGroup)?.trim()
            val balance = AmountParser.parse(extract(body, rule.balanceRegex, rule.balanceGroup))
            // Prefer a provider captured from the body (e.g. the funding wallet in
            // a Lipa Kwa Simu message); otherwise use the rule's fixed provider slug.
            val provider = normalizeProvider(extract(body, rule.providerRegex, rule.providerGroup))
                ?: rule.provider

            return ParseResult(
                provider = provider,
                direction = rule.direction,
                amount = amount,
                counterpartyName = name?.ifBlank { null },
                counterpartyNumber = number,
                reference = reference?.ifBlank { null },
                balanceAfter = balance,
                matchedRuleId = rule.id,
                matchedRuleName = rule.name
            )
        }
        return null
    }

    /**
     * Best-effort extraction for a message we already know is a payment (its
     * sender is a known money/bank ID) but which no structured rule matched —
     * e.g. the operator tweaked its wording. We pull whatever we can (amount,
     * payer name/number) using generic patterns so the payment is still recorded
     * and forwarded. [amount] may come back null; the caller forwards it anyway.
     */
    fun looseParse(body: String, provider: String?): ParseResult {
        val amount = AmountParser.parse(
            extract(body, "(?i)(?:Tsh|TZS|TSH|Tzs|TSHS|Shs?)\\.?\\s*([\\d.,]+)", 1)
                ?: extract(body, "(\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?)", 1)
        )
        val number = normalizeNumber(extract(body, GENERIC_PHONE, 1))
        val name = extract(
            body,
            "(?i)(?:kutoka(?: kwa)?|from)\\s+([A-Za-z][A-Za-z'. ]{1,40}?)\\s*(?=\\(|;|-|\\d|$)",
            1
        )?.trim()?.trim('.', ',')
        val reference = extract(body, "(?i)(?:Kumbukumbu|Ref(?:erence)?|TID|Muamala)\\s*No?\\.?:?\\s*([A-Za-z0-9]{6,20})", 1)
        return ParseResult(
            provider = provider ?: "unknown",
            direction = "RECEIVED",
            amount = amount,
            counterpartyName = name?.ifBlank { null },
            counterpartyNumber = number,
            reference = reference?.ifBlank { null },
            balanceAfter = null,
            matchedRuleId = -1L,
            matchedRuleName = "Auto (financial sender)"
        )
    }

    private fun extract(text: String, pattern: String?, group: Int): String? {
        if (pattern.isNullOrBlank()) return null
        val regex = safeRegex(pattern) ?: return null
        val match = regex.find(text) ?: return null
        // groupValues is a List (index 0 = whole match); fall back to the full
        // match when the requested capture group is absent or empty.
        val captured = match.groupValues.getOrNull(group)?.takeIf { it.isNotBlank() }
        return captured ?: match.groupValues.firstOrNull()?.takeIf { it.isNotBlank() }
    }

    /** Turns a captured wallet name into a compact slug, e.g. "Airtel Money" -> "airtelmoney". */
    private fun normalizeProvider(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val slug = raw.trim().lowercase()
            .replace("-", "")
            .replace(Regex("[^a-z0-9]"), "")
        return slug.ifBlank { null }
    }

    private fun normalizeNumber(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val digits = raw.filter { it.isDigit() || it == '+' }
        return when {
            digits.startsWith("+") -> digits
            digits.startsWith("255") -> "+$digits"
            digits.startsWith("0") && digits.length == 10 -> "+255${digits.substring(1)}"
            else -> digits
        }
    }

    private companion object {
        const val GENERIC_PHONE = "(\\+?255\\d{9}|0\\d{9})"
    }

    private fun safeRegex(pattern: String): Regex? = try {
        Regex(pattern, RegexOption.IGNORE_CASE)
    } catch (e: Exception) {
        null
    }
}
