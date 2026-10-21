package dev.esmanhoto.leantv

import android.content.Context
import android.content.SharedPreferences

class FavoritesStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("leantv", Context.MODE_PRIVATE)

    fun load(): List<String> =
        Favorites.deserialize(prefs.getString(KEY, null)) ?: Favorites.DEFAULTS

    fun save(favoritePackages: List<String>) {
        prefs.edit().putString(KEY, Favorites.serialize(favoritePackages)).apply()
    }

    fun toggle(packageName: String): List<String> =
        Favorites.toggle(load(), packageName).also { save(it) }

    private companion object {
        const val KEY = "favorites"
    }
}
