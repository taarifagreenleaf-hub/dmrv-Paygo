package com.greenleaf.paygo.parser

/** Structured output of parsing a payment SMS against a [com.greenleaf.paygo.data.db.entity.ParsingRuleEntity]. */
data class ParseResult(
    val provider: String,
    val direction: String,
    val amount: Double?,
    val counterpartyName: String?,
    val counterpartyNumber: String?,
    val reference: String?,
    val balanceAfter: Double?,
    val matchedRuleId: Long,
    val matchedRuleName: String
) {
    /** A result is only useful as a payment if we at least got an amount. */
    val isPayment: Boolean get() = amount != null
}
