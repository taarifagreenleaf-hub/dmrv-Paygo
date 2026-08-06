package com.greenleaf.paygo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerIdTest {
    @Test fun `formats padded id`() {
        assertEquals("CUST-0001", CustomerId.format("CUST", 1))
        assertEquals("CUST-0042", CustomerId.format("cust", 42))
    }

    @Test fun `detects id in replies`() {
        assertEquals(1, CustomerId.detectSeq("CUST-0001 jina Asha, nipo Mbeya", "CUST"))
        assertEquals(1, CustomerId.detectSeq("cust 1 Asha Mbeya", "CUST"))
        assertEquals(42, CustomerId.detectSeq("my id is CUST0042 thanks", "CUST"))
        assertEquals(7, CustomerId.detectSeq("CUST-7", "CUST"))
    }

    @Test fun `no id returns null`() {
        assertNull(CustomerId.detectSeq("Nimelipa 50000 asante", "CUST"))
    }
}
