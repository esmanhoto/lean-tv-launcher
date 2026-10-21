package dev.esmanhoto.leantv

/**
 * WMO weather interpretation codes as used by Open-Meteo:
 * https://open-meteo.com/en/docs#weather_variable_documentation
 */
object WeatherCodes {

    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67 -> "Rain"
        71, 73, 75, 77 -> "Snow"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> ""
    }

    /** Coarse buckets used to pick a weather-matching wallpaper variant. */
    enum class Bucket { CLEAR, RAIN, SNOW }

    fun bucket(code: Int): Bucket = when (code) {
        71, 73, 75, 77, 85, 86 -> Bucket.SNOW
        in 51..67, in 80..82, 95, 96, 99 -> Bucket.RAIN
        else -> Bucket.CLEAR
    }

    fun glyph(code: Int): String = when (code) {
        0, 1 -> "☀"
        2 -> "⛅"
        3 -> "☁"
        45, 48 -> "🌫"
        51, 53, 55, 56, 57 -> "🌦"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧"
        71, 73, 75, 77, 85, 86 -> "🌨"
        95, 96, 99 -> "⛈"
        else -> ""
    }
}
