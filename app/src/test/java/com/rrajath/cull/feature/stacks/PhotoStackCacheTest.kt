package com.rrajath.cull.feature.stacks

import android.net.Uri
import com.rrajath.cull.core.model.UnifiedPhotoItem
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoStackCacheTest {

    private val mockUri = mockk<Uri>(relaxed = true)

    @After
    fun tearDown() {
        PhotoStackCache.stacks = emptyMap()
    }

    @Test
    fun `stacks is initially empty`() {
        assertTrue(PhotoStackCache.stacks.isEmpty())
    }

    @Test
    fun `stores and retrieves stacks`() {
        val photos = listOf(
            UnifiedPhotoItem(id = "1", uri = mockUri, name = "1.jpg")
        )
        PhotoStackCache.stacks = mapOf(0 to photos)
        assertEquals(1, PhotoStackCache.stacks.size)
        assertEquals("1", PhotoStackCache.stacks[0]?.get(0)?.id)
    }

    @Test
    fun `overwrites existing stacks`() {
        PhotoStackCache.stacks = mapOf(
            0 to listOf(UnifiedPhotoItem(id = "old", uri = mockUri, name = "old.jpg"))
        )
        PhotoStackCache.stacks = mapOf(
            1 to listOf(UnifiedPhotoItem(id = "new", uri = mockUri, name = "new.jpg"))
        )
        assertEquals(1, PhotoStackCache.stacks.size)
        assertEquals("new", PhotoStackCache.stacks[1]?.get(0)?.id)
    }

    @Test
    fun `stores multiple stacks`() {
        PhotoStackCache.stacks = mapOf(
            0 to listOf(UnifiedPhotoItem(id = "a", uri = mockUri, name = "a.jpg")),
            1 to listOf(UnifiedPhotoItem(id = "b", uri = mockUri, name = "b.jpg")),
        )
        assertEquals(2, PhotoStackCache.stacks.size)
    }

    @Test
    fun `removePhotos scrubs matching ids from every stack`() {
        PhotoStackCache.stacks = mapOf(
            0 to listOf(
                UnifiedPhotoItem(id = "a", uri = mockUri, name = "a.jpg"),
                UnifiedPhotoItem(id = "b", uri = mockUri, name = "b.jpg"),
            ),
            1 to listOf(
                UnifiedPhotoItem(id = "c", uri = mockUri, name = "c.jpg"),
            ),
        )
        PhotoStackCache.removePhotos(setOf("b", "c"))
        assertEquals(1, PhotoStackCache.stacks[0]?.size)
        assertEquals("a", PhotoStackCache.stacks[0]?.get(0)?.id)
        assertTrue(PhotoStackCache.stacks[1].orEmpty().isEmpty())
    }

    @Test
    fun `removePhotos with empty set is a no-op`() {
        PhotoStackCache.stacks = mapOf(
            0 to listOf(UnifiedPhotoItem(id = "a", uri = mockUri, name = "a.jpg"))
        )
        PhotoStackCache.removePhotos(emptySet())
        assertEquals(1, PhotoStackCache.stacks[0]?.size)
    }
}
