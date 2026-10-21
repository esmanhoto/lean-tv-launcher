package dev.esmanhoto.leantv

import org.json.JSONObject
import kotlin.math.roundToInt

object Weather {

    data class Current(
        val temperatureC: Double,
        val code: Int,
        val sunrise: String? = null,
        val sunset: String? = null,
    ) {
        /** e.g. "☀ 3° Clear" — glyph/description omitted for unknown codes. */
        fun display(): String {
            val temp = "${temperatureC.roundToInt()}°"
            val glyph = WeatherCodes.glyph(code)
            val text = WeatherCodes.describe(code)
            return listOf(glyph, temp, text).filter { it.isNotEmpty() }.joinToString(" ")
        }

        /** e.g. "08:12 → 17:49", or null when the API gave no daily data. */
        fun daylightDisplay(): String? =
            if (sunrise != null && sunset != null) "$sunrise → $sunset" else null
    }

    fun url(latitude: Double, longitude: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current=temperature_2m,weather_code" +
            "&daily=sunrise,sunset&timezone=auto&forecast_days=1"

    fun parse(json: String): Current {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        fun clockTime(key: String): String? =
            daily?.optJSONArray(key)?.optString(0, null)
                ?.substringAfter('T', "")?.take(5)?.takeIf { it.length == 5 }
        return Current(
            temperatureC = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            sunrise = clockTime("sunrise"),
            sunset = clockTime("sunset"),
        )
    }

    /** Fetches on a background thread; [callback] runs on that thread, null on any failure. */
    fun fetch(latitude: Double, longitude: Double, callback: (Current?) -> Unit) {
        Thread {
            callback(Http.get(url(latitude, longitude))?.let { json ->
                try { parse(json) } catch (e: Exception) { null }
            })
        }.start()
    }
}
