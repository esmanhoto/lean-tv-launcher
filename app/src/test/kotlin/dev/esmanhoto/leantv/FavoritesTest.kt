package dev.esmanhoto.leantv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesTest {

    private fun app(pkg: String, leanback: Boolean = true) =
        AppEntry(pkg, label = pkg.substringAfterLast('.'), leanback = leanback)

    @Test
    fun `compose keeps saved order and drops uninstalled`() {
        val installed = listOf(app("a"), app("b"), app("c"))
        val favorites = listOf("c", "gone", "a")
        assertEquals(listOf("c", "a"), Favorites.compose(installed, favorites).map { it.packageName })
    }

    @Test
    fun `compose falls back to first leanback apps when nothing matches`() {
        val installed = listOf(app("sideloaded", leanback = false), app("tv1"), app("tv2"))
        val result = Favorites.compose(installed, listOf("gone"), fallbackCount = 2)
        assertEquals(listOf("tv1", "tv2"), result.map { it.packageName })
    }

    @Test
    fun `toggle adds missing package at the end`() {
        assertEquals(listOf("a", "b"), Favorites.toggle(listOf("a"), "b"))
    }

    @Test
    fun `toggle removes present package`() {
        assertEquals(listOf("a"), Favorites.toggle(listOf("a", "b"), "b"))
    }

    @Test
    fun `serialize and deserialize round-trip`() {
        val favorites = listOf("com.netflix.ninja", "com.google.android.youtube.tv")
        assertEquals(favorites, Favorites.deserialize(Favorites.serialize(favorites)))
    }

    @Test
    fun `deserialize of null is null so defaults apply`() {
        assertNull(Favorites.deserialize(null))
    }

    @Test
    fun `deserialize skips empty segments`() {
        assertEquals(listOf("a", "b"), Favorites.deserialize("a,, b ,"))
    }
}
