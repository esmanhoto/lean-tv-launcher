package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTest {

    @Test
    fun `parses real open-meteo response shape with daily sun times`() {
        val json = """
            {
              "latitude": 60.17,
              "longitude": 24.94,
              "current_units": {"temperature_2m": "°C", "weather_code": "wmo code"},
              "current": {"time": "2026-10-21T10:30", "interval": 900,
                          "temperature_2m": -3.6, "weather_code": 71},
              "daily": {"time": ["2026-10-21"],
                        "sunrise": ["2026-10-21T08:12"], "sunset": ["2026-10-21T17:49"]}
            }
        """.trimIndent()
        val current = Weather.parse(json)
        assertEquals(-3.6, current.temperatureC, 0.001)
        assertEquals(71, current.code)
        assertEquals("08:12", current.sunrise)
        assertEquals("17:49", current.sunset)
        assertEquals("08:12 → 17:49", current.daylightDisplay())
    }

    @Test
    fun `parse survives a response without daily data`() {
        val json = """{"current": {"temperature_2m": 2.0, "weather_code": 0}}"""
        val current = Weather.parse(json)
        assertEquals(null, current.sunrise)
        assertEquals(null, current.daylightDisplay())
    }

    @Test
    fun `display rounds temperature and includes description`() {
        assertEquals("🌨 -4° Snow", Weather.Current(-3.6, 71).display())
    }

    @Test
    fun `display omits glyph and text for unknown codes`() {
        assertEquals("12°", Weather.Current(12.4, 42).display())
    }

    @Test
    fun `url contains coordinates and requested fields`() {
        val url = Weather.url(60.17, 24.94)
        assertEquals(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=60.17&longitude=24.94&current=temperature_2m,weather_code" +
                "&daily=sunrise,sunset&timezone=auto&forecast_days=1",
            url,
        )
    }
}
