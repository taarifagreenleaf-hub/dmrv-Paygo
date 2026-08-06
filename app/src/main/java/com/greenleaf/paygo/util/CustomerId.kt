package com.greenleaf.paygo.util

/** Formats and detects the customer identifier (sms-cust-id, e.g. "CUST-0001"). */
object CustomerId {

    /** Builds the display id, e.g. format("CUST", 1) -> "CUST-0001". */
    fun format(prefix: String, seq: Int, pad: Int = 4): String =
        "${prefix.trim().uppercase()}-${seq.toString().padStart(pad, '0')}"

    /**
     * Finds the numeric sequence of a customer id mentioned in a message body.
     * Tolerates spacing/casing and leading zeros: "CUST-0001", "cust 1",
     * "CUST0001" all resolve to 1. Returns null if none is present.
     */
    fun detectSeq(body: String, prefix: String): Int? {
        val p = Regex.escape(prefix.trim())
        val rx = Regex("(?i)\\b$p[\\s\\-_]?0*(\\d{1,6})\\b")
        return rx.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }
}
