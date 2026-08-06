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

            return ParseResult(
                provider = rule.provider,
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

    private fun extract(text: String, pattern: String?, group: Int): String? {
        if (pattern.isNullOrBlank()) return null
        val regex = safeRegex(pattern) ?: return null
        val match = regex.find(text) ?: return null
        // groupValues is a List (index 0 = whole match); fall back to the full
        // match when the requested capture group is absent or empty.
        val captured = match.groupValues.getOrNull(group)?.takeIf { it.isNotBlank() }
        return captured ?: match.groupValues.firstOrNull()?.takeIf { it.isNotBlank() }
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

    private fun safeRegex(pattern: String): Regex? = try {
        Regex(pattern, RegexOption.IGNORE_CASE)
    } catch (e: Exception) {
        null
    }
}
