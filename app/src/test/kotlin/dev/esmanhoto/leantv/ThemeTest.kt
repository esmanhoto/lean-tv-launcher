package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun `unknown or missing stored name falls back to default theme`() {
        assertEquals(Theme.MEADOW, Theme.from(null))
        assertEquals(Theme.MEADOW, Theme.from("NOT_A_THEME"))
    }

    @Test
    fun `stored name round-trips`() {
        for (theme in Theme.entries) {
            assertEquals(theme, Theme.from(theme.name))
        }
    }
}
