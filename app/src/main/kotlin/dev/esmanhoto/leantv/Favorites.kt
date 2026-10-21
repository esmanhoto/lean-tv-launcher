package dev.esmanhoto.leantv

/**
 * Pure logic for the favorites row. Persistence lives in [FavoritesStore];
 * everything here is a plain function so it can be unit-tested on the JVM.
 */
object Favorites {

    /** Seed row for a fresh install. Entries not installed are simply skipped. */
    val DEFAULTS = listOf(
        "com.netflix.ninja",
        "com.google.android.youtube.tv",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "com.wbd.stream",
        "fi.yle.areena.tv",
        "com.globo.globotv",
        "com.surfshark.vpnclient.android",
    )

    /**
     * Resolves the saved favorite package names against the installed apps,
     * keeping the saved order and dropping anything no longer installed.
     * Falls back to the first [fallbackCount] installed apps when nothing matches.
     */
    fun compose(
        installed: List<AppEntry>,
        favoritePackages: List<String>,
        fallbackCount: Int = 6,
    ): List<AppEntry> {
        val byPackage = installed.associateBy { it.packageName }
        val resolved = favoritePackages.mapNotNull { byPackage[it] }
        if (resolved.isNotEmpty()) return resolved
        return installed.filter { it.leanback }.take(fallbackCount)
    }

    fun toggle(favoritePackages: List<String>, packageName: String): List<String> =
        if (packageName in favoritePackages) favoritePackages - packageName
        else favoritePackages + packageName

    fun serialize(favoritePackages: List<String>): String =
        favoritePackages.joinToString(",")

    fun deserialize(raw: String?): List<String>? =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
}
