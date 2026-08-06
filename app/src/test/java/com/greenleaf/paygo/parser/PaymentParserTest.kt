package com.greenleaf.paygo.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountParserTest {
    @Test fun `comma thousands with decimals`() {
        assertEquals(50000.0, AmountParser.parse("50,000.00")!!, 0.001)
    }

    @Test fun `dot thousands`() {
        assertEquals(1250000.0, AmountParser.parse("1.250.000")!!, 0.001)
    }

    @Test fun `plain integer`() {
        assertEquals(10000.0, AmountParser.parse("10000")!!, 0.001)
    }

    @Test fun `blank is null`() {
        assertNull(AmountParser.parse(""))
    }
}

class PaymentParserTest {

    private val parser = PaymentParser()
    private val rules = DefaultRules.parsingRules()

    @Test fun `parses M-Pesa received message`() {
        val body = "ABC1234567 Confirmed. You have received Tsh 50,000.00 from JOHN DOE " +
            "255712345678 on 6/8/26 at 10:00 AM. New M-PESA balance is Tsh 120,000.00"
        val result = parser.parse("M-PESA", body, rules)

        assertNotNull(result)
        assertTrue(result!!.isPayment)
        assertEquals("mpesa", result.provider)
        assertEquals(50000.0, result.amount!!, 0.001)
        assertEquals("+255712345678", result.counterpartyNumber)
    }

    @Test fun `non-payment sender does not match payment rules`() {
        val result = parser.parse("Mom", "Uko wapi? Nakuja jioni.", rules)
        assertNull(result)
    }
}
