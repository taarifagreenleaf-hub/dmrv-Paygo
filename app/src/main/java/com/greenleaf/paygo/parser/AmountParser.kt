package com.greenleaf.paygo.parser

/** Normalises money strings like "50,000.00", "1.250.000", "10 000" to a Double. */
object AmountParser {

    fun parse(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        var s = raw.trim().replace(" ", "")
        val hasComma = s.contains(',')
        val hasDot = s.contains('.')

        s = when {
            // Both separators present: the last one is the decimal separator.
            hasComma && hasDot -> {
                if (s.lastIndexOf('.') > s.lastIndexOf(',')) {
                    s.replace(",", "")               // 50,000.00 -> 50000.00
                } else {
                    s.replace(".", "").replace(',', '.') // 50.000,00 -> 50000.00
                }
            }
            // Only comma: decimal if it's a 1-2 digit trailing group, else thousands.
            hasComma -> {
                val idx = s.lastIndexOf(',')
                val decimals = s.length - idx - 1
                if (decimals in 1..2 && s.count { it == ',' } == 1) s.replace(',', '.')
                else s.replace(",", "")
            }
            // Only dot: decimal if a single 1-2 digit trailing group, else thousands.
            hasDot -> {
                val idx = s.lastIndexOf('.')
                val decimals = s.length - idx - 1
                if (decimals in 1..2 && s.count { it == '.' } == 1) s
                else s.replace(".", "")
            }
            else -> s
        }
        return s.toDoubleOrNull()
    }
}
