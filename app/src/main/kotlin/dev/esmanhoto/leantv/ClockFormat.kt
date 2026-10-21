package dev.esmanhoto.leantv

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ClockFormat {

    fun time(now: LocalDateTime, is24Hour: Boolean, locale: Locale): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, locale).format(now)
    }

    fun date(now: LocalDateTime, locale: Locale): String =
        DateTimeFormatter.ofPattern("EEE d MMM", locale).format(now)
}
