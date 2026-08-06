package com.greenleaf.paygo.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InfoExtractorTest {
    private val extractor = InfoExtractor()
    private val rules = DefaultRules.infoRules()

    @Test fun `captures name location amount system size`() {
        val body = "Habari, jina langu Asha Mjape, nipo Mbeya. Nimelipa 80,000 kwa system 200W. 0712254863"
        val info = extractor.extract(body, rules)
        assertEquals("Asha Mjape", info["name"])
        assertTrue(info["location"]!!.contains("Mbeya"))
        assertEquals("80,000", info["amount"])
        assertTrue(info["system_size"]!!.lowercase().contains("200"))
        assertEquals("0712254863", info["phone"])
    }

    @Test fun `empty when no info present`() {
        val info = extractor.extract("sawa asante", rules)
        assertTrue(info.isEmpty() || !info.containsKey("name"))
    }
}
