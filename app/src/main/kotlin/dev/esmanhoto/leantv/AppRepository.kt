package dev.esmanhoto.leantv

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

class AppRepository(private val context: Context) {

    /** All launchable apps, leanback entries preferred, sorted by label. */
    fun loadApps(): List<AppEntry> {
        val pm = context.packageManager
        val seen = LinkedHashMap<String, AppEntry>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val probe = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(probe, 0)) {
                val packageName = info.activityInfo.packageName
                if (packageName == context.packageName || seen.containsKey(packageName)) continue
                seen[packageName] = AppEntry(
                    packageName = packageName,
                    label = info.loadLabel(pm).toString(),
                    leanback = category == Intent.CATEGORY_LEANBACK_LAUNCHER,
                )
            }
        }
        return seen.values.sortedBy { it.label.lowercase() }
    }

    fun banner(packageName: String): Drawable? = try {
        context.packageManager.getApplicationBanner(packageName)
    } catch (e: Exception) {
        null
    }

    fun icon(packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    fun launchIntent(packageName: String): Intent? {
        val pm = context.packageManager
        return pm.getLeanbackLaunchIntentForPackage(packageName)
            ?: pm.getLaunchIntentForPackage(packageName)
    }
}
