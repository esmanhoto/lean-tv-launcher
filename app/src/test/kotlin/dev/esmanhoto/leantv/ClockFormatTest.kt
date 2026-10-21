package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class ClockFormatTest {

    private val evening: LocalDateTime = LocalDateTime.of(2026, 10, 21, 21, 5)

    @Test
    fun `24h time keeps leading zero minutes`() {
        assertEquals("21:05", ClockFormat.time(evening, is24Hour = true, locale = Locale.UK))
    }

    @Test
    fun `12h time uses am-pm`() {
        assertEquals("9:05 PM", ClockFormat.time(evening, is24Hour = false, locale = Locale.US))
    }

    @Test
    fun `date is weekday day month`() {
        assertEquals("Wed 21 Oct", ClockFormat.date(evening, Locale.UK))
    }
}
