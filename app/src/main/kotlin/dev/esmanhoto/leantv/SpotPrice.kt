package dev.esmanhoto.leantv

import org.json.JSONObject
import java.util.Locale

/** Finnish electricity spot price, via the free keyless spot-hinta.fi API. */
object SpotPrice {

    const val URL = "https://api.spot-hinta.fi/JustNow"

    /** Returns the current price in cents/kWh (the API reports EUR/kWh, VAT included). */
    fun parse(json: String): Double = JSONObject(json).getDouble("PriceWithTax") * 100

    fun display(centsPerKwh: Double): String =
        String.format(Locale.US, "%.1f c/kWh", centsPerKwh)

    fun fetch(callback: (Double?) -> Unit) {
        Thread {
            callback(Http.get(URL)?.let { json ->
                try { parse(json) } catch (e: Exception) { null }
            })
        }.start()
    }
}
