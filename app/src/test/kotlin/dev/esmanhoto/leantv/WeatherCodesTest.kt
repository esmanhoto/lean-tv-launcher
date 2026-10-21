package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodesTest {

    @Test
    fun `all documented wmo codes have a description and glyph`() {
        val documented = listOf(
            0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67,
            71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99,
        )
        for (code in documented) {
            assertTrue("missing description for $code", WeatherCodes.describe(code).isNotEmpty())
            assertTrue("missing glyph for $code", WeatherCodes.glyph(code).isNotEmpty())
        }
    }

    @Test
    fun `unknown codes degrade to empty strings`() {
        assertEquals("", WeatherCodes.describe(42))
        assertEquals("", WeatherCodes.glyph(42))
    }

    @Test
    fun `codes bucket into clear, rain and snow for wallpaper variants`() {
        assertEquals(WeatherCodes.Bucket.CLEAR, WeatherCodes.bucket(0))
        assertEquals(WeatherCodes.Bucket.CLEAR, WeatherCodes.bucket(3))
        assertEquals(WeatherCodes.Bucket.CLEAR, WeatherCodes.bucket(45))
        assertEquals(WeatherCodes.Bucket.RAIN, WeatherCodes.bucket(61))
        assertEquals(WeatherCodes.Bucket.RAIN, WeatherCodes.bucket(81))
        assertEquals(WeatherCodes.Bucket.RAIN, WeatherCodes.bucket(95))
        assertEquals(WeatherCodes.Bucket.SNOW, WeatherCodes.bucket(71))
        assertEquals(WeatherCodes.Bucket.SNOW, WeatherCodes.bucket(86))
        assertEquals(WeatherCodes.Bucket.CLEAR, WeatherCodes.bucket(42))
    }
}
