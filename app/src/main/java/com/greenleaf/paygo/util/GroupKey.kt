package com.greenleaf.paygo.util

/** Derives a stable bucket key so messages from the same payee/sender group together. */
object GroupKey {

    /** For normal messages: normalise the sender line. */
    fun forSender(address: String): String =
        address.trim().lowercase().replace(Regex("[^a-z0-9+]"), "")

    /**
     * For payments: prefer the counterparty phone number, then the name, then the
     * raw sender line, so repeated payments from one person collapse into one group.
     */
    fun forCounterparty(number: String?, name: String?, address: String): String = when {
        !number.isNullOrBlank() -> number.filter { it.isDigit() || it == '+' }
        !name.isNullOrBlank() -> "name:" + name.trim().lowercase().replace(Regex("\\s+"), " ")
        else -> forSender(address)
    }
}
