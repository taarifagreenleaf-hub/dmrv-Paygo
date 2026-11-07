package com.greenleaf.paygo.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the guarantee that 100% of payment SMS from every supported provider
 * are recognised (and therefore forwarded), even when the operator's wording
 * drifts away from the structured parsing rules.
 */
class PaymentSendersTest {

    private val parser = PaymentParser()
    private val rules = DefaultRules.parsingRules()

    /** Mirrors MessageProcessor's "is this a payment SMS that must forward?" decision. */
    private fun isPaymentSms(address: String, body: String): Boolean {
        val structured = parser.parse(address, body, rules)
        return structured?.isPayment == true || PaymentSenders.isPaymentSms(address, body)
    }

    @Test fun `detects every provider by sender id`() {
        val cases = mapOf(
            "M-PESA" to "mpesa",
            "MIXX BY YAS" to "mixx",
            "Yas" to "mixx",
            "TIGOPESA" to "mixx",
            "AIRTELMONEY" to "airtelmoney",
            "Airtel Money" to "airtelmoney",
            "HaloPesa" to "halopesa",
            "T-PESA" to "tpesa",
            "TTCL" to "tpesa",
            "SELCOM" to "selcom",
            "CRDB" to "crdb",
            "NMB" to "nmb",
            "NBC" to "nbc"
        )
        for ((sender, slug) in cases) {
            assertEquals("sender=$sender", slug, PaymentSenders.detect(sender))
        }
    }

    @Test fun `customer phone number is not a financial sender`() {
        assertNull(PaymentSenders.detect("+255712345678"))
        assertNull(PaymentSenders.detect("0712345678"))
        assertFalse(isPaymentSms("+255712345678", "Nimelipa 50,000 jina Asha nipo Mbeya"))
    }

    @Test fun `mixx by yas with drifted wording is still a payment`() {
        val body = "Muamala umekamilika. Umepokea TZS 15,000 toka JUMA KASSIM. Salio 20,000. TID AB12CD34"
        assertTrue(isPaymentSms("MIXX BY YAS", body))
    }

    @Test fun `every provider payment is forwarded even if unparsed`() {
        val senders = listOf(
            "M-PESA", "MIXX BY YAS", "AIRTELMONEY", "HaloPesa",
            "T-PESA", "SELCOM", "CRDB", "NMB"
        )
        val body = "Taarifa: umepokea Tsh 7,500 leo. Asante."
        for (s in senders) assertTrue("provider $s must forward", isPaymentSms(s, body))
    }

    @Test fun `promo without amount or keyword is not treated as payment`() {
        assertFalse(isPaymentSms("M-PESA", "Bonyeza *150*00# kuangalia ofa zetu mpya."))
    }

    @Test fun `loose parse extracts amount and number from drifted body`() {
        val r = parser.looseParse("Umepokea TZS 15,000 toka JUMA KASSIM 255713111222. TID AB12", "mixx")
        assertEquals(15000.0, r.amount!!, 0.001)
        assertEquals("mixx", r.provider)
        assertEquals("+255713111222", r.counterpartyNumber)
    }

    @Test fun `loose parse survives when no amount is present`() {
        val r = parser.looseParse("Malipo yamepokelewa, angalia akaunti yako.", "crdb")
        assertNull(r.amount)
        assertEquals("crdb", r.provider)
        assertNotNull(r.matchedRuleName)
    }
}
