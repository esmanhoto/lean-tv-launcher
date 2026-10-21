package dev.esmanhoto.leantv

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.format.DateFormat
import android.text.style.MetricAffectingSpan
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.time.LocalDateTime
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var repository: AppRepository
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var themeStore: ThemeStore
    private var theme: Theme = Theme.MEADOW

    private lateinit var widgetStack: View
    private lateinit var widgetRows: GridLayout
    private lateinit var timeView: TextView
    private lateinit var dateView: TextView
    private lateinit var weatherView: TextView
    private lateinit var sunView: TextView
    private lateinit var powerView: TextView
    private var wallpaperBucket: WeatherCodes.Bucket? = null
    private var wallpaperDrawable: Drawable? = null
    private var liveBucket: WeatherCodes.Bucket = WeatherCodes.Bucket.CLEAR
    private var forcedBucket: WeatherCodes.Bucket? = null
    private var defaultTimeTypeface: Typeface? = null
    private var defaultTextTypeface: Typeface? = null
    private lateinit var favoritesScroll: ViewGroup
    private lateinit var favoritesRow: GridLayout
    private var favoritesRestingY = 0f
    private lateinit var allAppsPanel: View
    private lateinit var allAppsGrid: GridLayout
    private var allAppsStale = true
    private lateinit var ambientScrim: View
    private var ambient = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private val clockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateClock()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = reloadApps()
    }

    private val weatherTicker = object : Runnable {
        override fun run() {
            val ticker = this
            Weather.fetch(latitude, longitude) { current ->
                mainHandler.post {
                    if (current != null) {
                        weatherView.text = current.display()
                        current.daylightDisplay()?.let { sunView.text = alignArrow(it) }
                        liveBucket = WeatherCodes.bucket(current.code)
                        if (!ambient) applyWallpaper(forcedBucket ?: liveBucket)
                    }
                    mainHandler.removeCallbacks(ticker)
                    mainHandler.postDelayed(
                        ticker,
                        if (current != null) WEATHER_INTERVAL_MS else WEATHER_RETRY_MS,
                    )
                }
            }
            SpotPrice.fetch { cents ->
                if (cents != null) {
                    mainHandler.post { powerView.text = "⚡ ${SpotPrice.display(cents)}" }
                }
            }
        }
    }

    /** Swaps the window background when the weather bucket (or theme) changes. */
    private fun applyWallpaper(bucket: WeatherCodes.Bucket, crossfade: Boolean = false) {
        if (bucket == wallpaperBucket) return
        wallpaperBucket = bucket
        // RGB_565 halves the decoded bitmap + GL texture vs the default ARGB_8888
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        val bitmap = BitmapFactory.decodeResource(resources, Wallpapers.res(this, theme, bucket), options)
        val next = BitmapDrawable(resources, bitmap)
        val previous = wallpaperDrawable
        wallpaperDrawable = next
        if (crossfade && previous != null) {
            val transition = TransitionDrawable(arrayOf(previous, next))
            window.setBackgroundDrawable(transition)
            transition.startTransition(CROSSFADE_MS)
        } else {
            window.setBackgroundDrawable(next)
        }
    }

    /** Long-press on the Theme tile: clear → rain → snow → back to live weather. */
    private fun cycleWallpaperPreview() {
        val buckets = WeatherCodes.Bucket.entries
        forcedBucket = when (val forced = forcedBucket) {
            null -> buckets.first()
            buckets.last() -> null
            else -> buckets[buckets.indexOf(forced) + 1]
        }
        applyWallpaper(forcedBucket ?: liveBucket)
        val message = forcedBucket
            ?.let { getString(R.string.wallpaper_preview, it.name.lowercase(Locale.US)) }
            ?: getString(R.string.wallpaper_live)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showThemeMenu() {
        val themes = Wallpapers.availableThemes(this)
        val labels = themes.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.theme)
            .setSingleChoiceItems(labels, themes.indexOf(theme)) { dialog, which ->
                theme = themes[which]
                themeStore.save(theme)
                val bucket = wallpaperBucket ?: WeatherCodes.Bucket.CLEAR
                wallpaperBucket = null
                applyWallpaper(bucket)
                applyThemeChrome()
                reloadApps()
                dialog.dismiss()
            }
            .show()
    }

    /** Applies the per-theme widget-box background and typography. */
    private fun applyThemeChrome() {
        widgetStack.setBackgroundResource(ThemeStyle.widgetScrim(theme))
        val mono = ThemeStyle.monospace(theme)
        timeView.typeface = if (mono) Typeface.MONOSPACE else defaultTimeTypeface
        val texts = mutableListOf(dateView)
        for (i in 0 until widgetRows.childCount) {
            (widgetRows.getChildAt(i) as? TextView)?.let { texts.add(it) }
        }
        for (view in texts) {
            view.typeface = if (mono) Typeface.MONOSPACE else defaultTextTypeface
        }
    }

    private val latitude by lazy { getString(R.string.weather_latitude).toDouble() }
    private val longitude by lazy { getString(R.string.weather_longitude).toDouble() }

    private val favoriteTileWidth by lazy {
        val padding = resources.getDimensionPixelSize(R.dimen.screen_padding)
        val spacing = resources.getDimensionPixelSize(R.dimen.tile_spacing)
        val available = resources.displayMetrics.widthPixels - 2 * padding
        (available - (FAVORITES_COLUMNS - 1) * spacing) / FAVORITES_COLUMNS
    }

    private val focusAnimator = View.OnFocusChangeListener { view, hasFocus ->
        val scale = if (hasFocus) 1.08f else 1f
        val alpha = if (hasFocus) 1f else TILE_REST_ALPHA
        view.animate().scaleX(scale).scaleY(scale).alpha(alpha).setDuration(120).start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AppRepository(this)
        favoritesStore = FavoritesStore(this)
        themeStore = ThemeStore(this)
        theme = themeStore.load().takeIf { Wallpapers.available(this, it) } ?: Theme.MEADOW
        applyWallpaper(WeatherCodes.Bucket.CLEAR)

        widgetStack = findViewById(R.id.widget_stack)
        widgetRows = findViewById(R.id.widget_rows)
        timeView = findViewById(R.id.time_view)
        dateView = findViewById(R.id.date_view)
        weatherView = findViewById(R.id.weather_view)
        sunView = findViewById(R.id.sun_view)
        powerView = findViewById(R.id.power_view)
        defaultTimeTypeface = timeView.typeface
        defaultTextTypeface = dateView.typeface
        applyThemeChrome()
        favoritesScroll = findViewById(R.id.favorites_scroll)
        favoritesRow = findViewById(R.id.favorites_row)
        allAppsPanel = findViewById(R.id.all_apps_panel)
        allAppsGrid = findViewById(R.id.all_apps_grid)
        ambientScrim = findViewById(R.id.ambient_scrim)

        registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        })

        reloadApps()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(clockReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        updateClock()
        mainHandler.post(weatherTicker)
        scheduleAmbient()
    }

    override fun onStop() {
        unregisterReceiver(clockReceiver)
        mainHandler.removeCallbacks(weatherTicker)
        exitAmbient()
        mainHandler.removeCallbacks(ambientStarter)
        // Free the all-apps banners while another app is in front; rebuilt on next open
        hideAllApps()
        allAppsGrid.removeAllViews()
        allAppsStale = true
        super.onStop()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // HOME pressed while already home: reset to the resting state
        hideAllApps()
        favoritesRow.getChildAt(0)?.requestFocus()
    }

    // Desktop-emulator convenience: some setups forward Esc as ESCAPE, not BACK
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ambient) {
            // The waking key press only wakes; swallow it so it doesn't act on a tile
            if (event.action == KeyEvent.ACTION_UP) exitAmbient()
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) hideAllApps()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (allAppsPanel.visibility == View.VISIBLE) {
            hideAllApps()
        }
        // else swallow: a home screen never finishes itself
    }

    /** Roboto's "→" sits below the digits' optical center; nudge it up. */
    private fun alignArrow(text: String): CharSequence {
        val i = text.indexOf('→')
        if (i < 0) return text
        return SpannableString(text).apply {
            setSpan(ArrowShiftSpan, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private object ArrowShiftSpan : MetricAffectingSpan() {
        private fun shift(paint: TextPaint) {
            paint.baselineShift -= (paint.textSize * 0.12f).toInt()
        }
        override fun updateDrawState(tp: TextPaint) = shift(tp)
        override fun updateMeasureState(tp: TextPaint) = shift(tp)
    }

    private fun updateClock() {
        val now = LocalDateTime.now()
        val locale = Locale.getDefault()
        timeView.text = ClockFormat.time(now, DateFormat.is24HourFormat(this), locale)
        dateView.text = ClockFormat.date(now, locale)
    }

    private fun reloadApps() {
        allAppsStale = true
        allAppsGrid.removeAllViews()
        Thread {
            val apps = repository.loadApps()
            val favorites = Favorites.compose(apps, favoritesStore.load())
            val banners = HashMap<String, Drawable?>()
            val icons = HashMap<String, Drawable?>()
            for (entry in favorites) {
                banners[entry.packageName] = repository.banner(entry.packageName)
                icons[entry.packageName] = repository.icon(entry.packageName)
            }
            runOnUiThread { populateFavorites(favorites, banners, icons) }
        }.start()
    }

    private fun loadAllApps(onDone: () -> Unit = {}) {
        allAppsStale = false
        Thread {
            val apps = repository.loadApps()
            val banners = HashMap<String, Drawable?>()
            val icons = HashMap<String, Drawable?>()
            for (entry in apps) {
                banners[entry.packageName] = repository.banner(entry.packageName)
                icons[entry.packageName] = repository.icon(entry.packageName)
            }
            runOnUiThread {
                allAppsGrid.removeAllViews()
                for (entry in apps) {
                    allAppsGrid.addView(appTile(allAppsGrid, entry, banners[entry.packageName], icons[entry.packageName], inGrid = true))
                }
                onDone()
            }
        }.start()
    }

    private fun populateFavorites(
        favorites: List<AppEntry>,
        banners: Map<String, Drawable?>,
        icons: Map<String, Drawable?>,
    ) {
        favoritesRow.removeAllViews()
        favoritesRow.columnCount = FAVORITES_COLUMNS
        for (entry in favorites) {
            favoritesRow.addView(appTile(favoritesRow, entry, banners[entry.packageName], icons[entry.packageName], inGrid = false))
        }
        favoritesRow.addView(specialTile(favoritesRow, "▦", getString(R.string.all_apps)) { showAllApps() })
        favoritesRow.addView(
            specialTile(
                favoritesRow, "✦", getString(R.string.theme),
                onLongClick = { cycleWallpaperPreview() },
            ) { showThemeMenu() }
        )
        favoritesRow.addView(
            specialTile(favoritesRow, "⚙", getString(R.string.settings)) {
                startActivitySafely(Intent(Settings.ACTION_SETTINGS))
            }
        )

        // Drop trailing margins so the grid reaches the screen edges exactly
        val columns = favoritesRow.columnCount
        val lastRowStart = ((favoritesRow.childCount - 1) / columns) * columns
        for (i in 0 until favoritesRow.childCount) {
            val child = favoritesRow.getChildAt(i)
            val lp = child.layoutParams as GridLayout.LayoutParams
            if ((i + 1) % columns == 0) lp.rightMargin = 0
            if (i >= lastRowStart) lp.bottomMargin = 0
            val row = i / columns
            child.setOnFocusChangeListener { v, hasFocus ->
                focusAnimator.onFocusChange(v, hasFocus)
                if (hasFocus) revealFavorites(row)
            }
        }

        val spacing = resources.getDimensionPixelSize(R.dimen.tile_spacing)
        favoritesRestingY =
            if (favoritesRow.childCount > columns) (favoriteTileWidth * 9 / 16 + spacing).toFloat() else 0f
        favoritesScroll.translationY = favoritesRestingY

        if (allAppsPanel.visibility == View.VISIBLE) {
            loadAllApps { allAppsGrid.getChildAt(0)?.requestFocus() }
        } else {
            favoritesRow.getChildAt(0)?.requestFocus()
        }
    }

    private fun appTile(
        parent: ViewGroup,
        entry: AppEntry,
        banner: Drawable?,
        icon: Drawable?,
        inGrid: Boolean,
    ): View {
        val tile = baseTile(parent, inGrid)
        val bannerView = tile.findViewById<ImageView>(R.id.tile_banner)
        if (banner != null) {
            bannerView.setImageDrawable(banner)
        } else {
            bannerView.visibility = View.GONE
            tile.findViewById<View>(R.id.tile_fallback).visibility = View.VISIBLE
            tile.findViewById<ImageView>(R.id.tile_icon).setImageDrawable(icon)
            tile.findViewById<TextView>(R.id.tile_label).text = entry.label
        }
        tile.setOnClickListener { launch(entry) }
        tile.setOnLongClickListener {
            showTileMenu(entry)
            true
        }
        return tile
    }

    private fun specialTile(
        parent: ViewGroup,
        glyph: String,
        label: String,
        onLongClick: (() -> Unit)? = null,
        onClick: () -> Unit,
    ): View {
        val tile = baseTile(parent, inGrid = false)
        tile.findViewById<View>(R.id.tile_banner).visibility = View.GONE
        tile.findViewById<View>(R.id.tile_fallback).visibility = View.VISIBLE
        tile.findViewById<View>(R.id.tile_icon).visibility = View.GONE
        tile.findViewById<TextView>(R.id.tile_glyph).apply {
            visibility = View.VISIBLE
            text = glyph
        }
        tile.findViewById<TextView>(R.id.tile_label).text = label
        tile.setOnClickListener { onClick() }
        if (onLongClick != null) {
            tile.setOnLongClickListener {
                onLongClick()
                true
            }
        }
        return tile
    }

    private fun baseTile(parent: ViewGroup, inGrid: Boolean): View {
        val tile = layoutInflater.inflate(R.layout.tile_app, parent, false)
        tile.setBackgroundResource(ThemeStyle.tileBackground(theme))
        tile.foreground = getDrawable(ThemeStyle.tileFocus(theme))
        if (ThemeStyle.monospace(theme)) {
            tile.findViewById<TextView>(R.id.tile_label).typeface = Typeface.MONOSPACE
            tile.findViewById<TextView>(R.id.tile_glyph).typeface = Typeface.MONOSPACE
        }
        tile.clipToOutline = true
        tile.alpha = TILE_REST_ALPHA
        tile.onFocusChangeListener = focusAnimator
        if (inGrid) {
            val margin = resources.getDimensionPixelSize(R.dimen.grid_tile_margin)
            tile.layoutParams = GridLayout.LayoutParams().apply {
                width = resources.getDimensionPixelSize(R.dimen.grid_tile_width)
                height = resources.getDimensionPixelSize(R.dimen.grid_tile_height)
                setMargins(margin, margin, margin, margin)
            }
        } else {
            val spacing = resources.getDimensionPixelSize(R.dimen.tile_spacing)
            tile.layoutParams = GridLayout.LayoutParams().apply {
                width = favoriteTileWidth
                height = favoriteTileWidth * 9 / 16
                setMargins(0, 0, spacing, spacing)
            }
        }
        return tile
    }

    private fun launch(entry: AppEntry) {
        val intent = repository.launchIntent(entry.packageName)
        if (intent == null) {
            Toast.makeText(this, R.string.cannot_launch, Toast.LENGTH_SHORT).show()
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        startActivitySafely(intent)
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.cannot_launch, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTileMenu(entry: AppEntry) {
        val isFavorite = entry.packageName in favoritesStore.load()
        val toggleLabel = getString(if (isFavorite) R.string.remove_favorite else R.string.add_favorite)
        AlertDialog.Builder(this)
            .setTitle(entry.label)
            .setItems(arrayOf(toggleLabel, getString(R.string.app_info))) { _, which ->
                when (which) {
                    0 -> {
                        favoritesStore.toggle(entry.packageName)
                        reloadApps()
                    }
                    1 -> startActivitySafely(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${entry.packageName}"),
                        )
                    )
                }
            }
            .show()
    }

    private val ambientStarter = Runnable { enterAmbient() }

    private val ambientRotator = object : Runnable {
        override fun run() {
            val buckets = WeatherCodes.Bucket.entries
            val current = buckets.indexOf(wallpaperBucket).coerceAtLeast(0)
            applyWallpaper(buckets[(current + 1) % buckets.size], crossfade = true)
            mainHandler.postDelayed(this, AMBIENT_ROTATE_MS)
        }
    }

    private fun scheduleAmbient() {
        mainHandler.removeCallbacks(ambientStarter)
        mainHandler.postDelayed(ambientStarter, AMBIENT_DELAY_MS)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (!ambient) scheduleAmbient()
    }

    /** Built-in screensaver: tiles fade out, screen dims, wallpapers rotate; clock stays. */
    private fun enterAmbient() {
        if (ambient) return
        if (!hasWindowFocus()) {
            // A dialog is up; its keys never reach us, so we couldn't wake. Try later.
            scheduleAmbient()
            return
        }
        ambient = true
        hideAllApps()
        favoritesScroll.animate().alpha(0f).setDuration(600).start()
        ambientScrim.visibility = View.VISIBLE
        ambientScrim.animate().alpha(AMBIENT_SCRIM_ALPHA).setDuration(600).start()
        window.attributes = window.attributes.apply { screenBrightness = AMBIENT_BRIGHTNESS }
        mainHandler.postDelayed(ambientRotator, AMBIENT_ROTATE_MS)
    }

    private fun exitAmbient() {
        if (!ambient) return
        ambient = false
        mainHandler.removeCallbacks(ambientRotator)
        favoritesScroll.animate().alpha(1f).setDuration(300).start()
        ambientScrim.animate().alpha(0f).setDuration(300)
            .withEndAction { ambientScrim.visibility = View.GONE }.start()
        window.attributes = window.attributes.apply {
            screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        applyWallpaper(forcedBucket ?: liveBucket)
        scheduleAmbient()
    }

    /** Rest state shows only the first favorites row; focusing a lower row slides the grid up. */
    private fun revealFavorites(row: Int) {
        val target = if (row == 0) favoritesRestingY else 0f
        if (favoritesScroll.translationY != target) {
            favoritesScroll.animate().translationY(target).setDuration(180).start()
        }
    }

    private fun showAllApps() {
        allAppsPanel.visibility = View.VISIBLE
        // Keep focus search from falling through the overlay onto the home tiles
        favoritesScroll.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        if (allAppsStale) {
            loadAllApps { allAppsGrid.getChildAt(0)?.requestFocus() }
        } else {
            allAppsGrid.getChildAt(0)?.requestFocus()
        }
    }

    private fun hideAllApps() {
        if (allAppsPanel.visibility == View.VISIBLE) {
            allAppsPanel.visibility = View.GONE
            favoritesScroll.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            favoritesRow.getChildAt(0)?.requestFocus()
        }
    }

    private companion object {
        const val WEATHER_INTERVAL_MS = 30L * 60 * 1000
        const val WEATHER_RETRY_MS = 30L * 1000
        const val FAVORITES_COLUMNS = 6
        const val TILE_REST_ALPHA = 0.8f
        const val AMBIENT_DELAY_MS = 10L * 60 * 1000
        const val AMBIENT_ROTATE_MS = 3L * 60 * 1000
        const val AMBIENT_SCRIM_ALPHA = 0.45f
        const val AMBIENT_BRIGHTNESS = 0.1f
        const val CROSSFADE_MS = 1200
    }
}
