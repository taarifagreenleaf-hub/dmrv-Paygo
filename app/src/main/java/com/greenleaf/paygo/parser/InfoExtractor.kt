package com.greenleaf.paygo.parser

import com.greenleaf.paygo.data.db.entity.InfoRuleEntity

/**
 * Pulls structured key info out of a free-form "normal" message using the user's
 * [InfoRuleEntity] rules. For each field key the highest-priority matching rule
 * wins. Returns an ordered map of fieldKey -> value (empty if nothing matched).
 */
class InfoExtractor {

    fun extract(body: String, rules: List<InfoRuleEntity>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for (rule in rules) {
            if (!rule.enabled) continue
            if (result.containsKey(rule.fieldKey)) continue // first (highest priority) wins
            val regex = safeRegex(rule.regex) ?: continue
            val match = regex.find(body) ?: continue
            val value = match.groupValues.getOrNull(rule.group)?.takeIf { it.isNotBlank() }
                ?: match.groupValues.firstOrNull()?.takeIf { it.isNotBlank() }
            if (value != null) result[rule.fieldKey] = value.trim().trim('.', ',', ';', '-').trim()
        }
        return result
    }

    private fun safeRegex(pattern: String): Regex? = try {
        Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    } catch (e: Exception) {
        null
    }
}
