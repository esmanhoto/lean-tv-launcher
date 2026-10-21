package dev.esmanhoto.leantv

import java.net.HttpURLConnection
import java.net.URL

object Http {
    /** Blocking GET; call from a background thread. Null on any failure. */
    fun get(url: String): String? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        null
    }
}
