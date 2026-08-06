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

    @Test fun `parses Lipa Kwa Simu with named wallet`() {
        val body = "Umepokea malipo TSh 100,000 kutoka kwa Airtel Money; 255694553433 - " +
            "MGENI HUSENI Kumbukumbu No.: 26793038832637. 06/08/26 18:24. " +
            "Salio lako jipya ni TSh 3,348,479. Asante kwa kutumia Lipa Kwa Simu.LKS"
        val r = parser.parse("Selcom", body, rules)
        assertNotNull(r)
        assertEquals("airtelmoney", r!!.provider)
        assertEquals(100000.0, r.amount!!, 0.001)
        assertEquals("MGENI HUSENI", r.counterpartyName)
        assertEquals("+255694553433", r.counterpartyNumber)
        assertEquals("26793038832637", r.reference)
        assertEquals(3348479.0, r.balanceAfter!!, 0.001)
    }

    @Test fun `parses Lipa Kwa Simu with vending alert prefix`() {
        val body = "Vending Alert SMS Selcom: Umepokea malipo TSh 100,000 kutoka kwa " +
            "Airtel Money; 255694553433 - MGENI HUSENI Kumbukumbu No.: 26187406394329. " +
            "06/08/26 17:47. Salio lako jipya ni TSh 3,148,479. Asante kwa kutumia Lipa Kwa Simu.LKS"
        val r = parser.parse("Selcom", body, rules)
        assertNotNull(r)
        assertEquals("airtelmoney", r!!.provider)
        assertEquals(100000.0, r.amount!!, 0.001)
        assertEquals("MGENI HUSENI", r.counterpartyName)
        assertEquals("26187406394329", r.reference)
    }

    @Test fun `parses Lipa namba direct number payment`() {
        val body = "Umepokea Malipo ya TSh 80,000 kwenye Lipa namba 459806551 kutoka kwa " +
            "255775998783 - ASHA MJAPE. Kumbukumbu No.: 26793037307782 06/08/26 16:12. " +
            "Salio lako jipya ni TSh 2,948,479. Asante kwa kutumia Lipa Kwa Simu.LKS"
        val r = parser.parse("Selcom", body, rules)
        assertNotNull(r)
        assertEquals("selcom", r!!.provider) // no named wallet -> fallback slug
        assertEquals(80000.0, r.amount!!, 0.001)
        assertEquals("ASHA MJAPE", r.counterpartyName)
        assertEquals("+255775998783", r.counterpartyNumber)
        assertEquals("26793037307782", r.reference)
        assertEquals(2948479.0, r.balanceAfter!!, 0.001)
    }

    @Test fun `parses Lipa namba with odd amount`() {
        val body = "Umepokea Malipo ya TSh 71,427 kwenye Lipa namba 459806551 kutoka kwa " +
            "255712254863 - ALLY ABDALA. Kumbukumbu No.: 26652888739246 06/08/26 09:57. " +
            "Salio lako jipya ni TSh 2,568,479. Asante kwa kutumia Lipa Kwa Simu.LKS"
        val r = parser.parse("Selcom", body, rules)
        assertNotNull(r)
        assertEquals(71427.0, r!!.amount!!, 0.001)
        assertEquals("ALLY ABDALA", r.counterpartyName)
        assertEquals("+255712254863", r.counterpartyNumber)
    }
}
