package dev.esmanhoto.leantv

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

enum class Theme(val label: String, val wallpaperPrefix: String) {
    MEADOW("Meadow", "wallpaper"),
    SHINOBI("Shinobi", "shinobi"),
    RETRO("Retro RPG", "retro");

    companion object {
        fun from(name: String?): Theme = entries.firstOrNull { it.name == name } ?: MEADOW
    }
}

/** Per-theme chrome: tile frames, widget-box background, typography. */
object ThemeStyle {
    fun widgetScrim(theme: Theme): Int =
        if (theme == Theme.RETRO) R.drawable.widget_scrim_retro else R.drawable.widget_scrim

    fun tileBackground(theme: Theme): Int =
        if (theme == Theme.RETRO) R.drawable.tile_bg_retro else R.drawable.tile_bg

    fun tileFocus(theme: Theme): Int =
        if (theme == Theme.RETRO) R.drawable.tile_focus_retro else R.drawable.tile_focus

    fun monospace(theme: Theme): Boolean = theme == Theme.RETRO
}

/**
 * Wallpapers are resolved by name (<prefix>_<bucket>.jpg in drawable-nodpi) so a
 * theme's images can live outside the repo: a theme is offered only when all
 * three weather variants exist in the build.
 */
object Wallpapers {
    @SuppressLint("DiscouragedApi")
    fun res(context: Context, theme: Theme, bucket: WeatherCodes.Bucket): Int =
        context.resources.getIdentifier(
            "${theme.wallpaperPrefix}_${bucket.name.lowercase(Locale.US)}",
            "drawable",
            context.packageName,
        )

    fun available(context: Context, theme: Theme): Boolean =
        WeatherCodes.Bucket.entries.all { res(context, theme, it) != 0 }

    fun availableThemes(context: Context): List<Theme> =
        Theme.entries.filter { available(context, it) }
}

class ThemeStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("leantv", Context.MODE_PRIVATE)

    fun load(): Theme = Theme.from(prefs.getString(KEY, null))

    fun save(theme: Theme) {
        prefs.edit().putString(KEY, theme.name).apply()
    }

    private companion object {
        const val KEY = "theme"
    }
}
