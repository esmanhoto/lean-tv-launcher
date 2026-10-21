package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotPriceTest {

    @Test
    fun `parses spot-hinta JustNow response to cents`() {
        val json = """{"Rank": 7, "DateTime": "2026-10-21T11:00:00+03:00", "PriceNoTax": 0.0340, "PriceWithTax": 0.0432}"""
        assertEquals(4.32, SpotPrice.parse(json), 0.0001)
    }

    @Test
    fun `display rounds to one decimal`() {
        assertEquals("4.3 c/kWh", SpotPrice.display(4.32))
        assertEquals("12.0 c/kWh", SpotPrice.display(11.96))
        assertEquals("-0.5 c/kWh", SpotPrice.display(-0.52))
    }
}
