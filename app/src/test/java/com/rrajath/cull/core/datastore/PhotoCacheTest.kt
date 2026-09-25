package com.rrajath.cull.core.datastore

import android.net.Uri
import com.rrajath.cull.core.model.PhotoSource
import com.rrajath.cull.core.model.UnifiedPhotoItem
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoCacheTest {

    private val mockUri1 = mockk<Uri>(relaxed = true)
    private val mockUri2 = mockk<Uri>(relaxed = true)
    private val mockUri3 = mockk<Uri>(relaxed = true)

    private val photo1 = UnifiedPhotoItem(
        id = "1", uri = mockUri1, name = "photo1.jpg",
        dateModified = 1000L, source = PhotoSource.Local
    )
    private val photo2 = UnifiedPhotoItem(
        id = "2", uri = mockUri2, name = "photo2.jpg",
        dateModified = 2000L, source = PhotoSource.Local
    )
    private val photos = listOf(photo1, photo2)

    @Before
    fun setUp() {
        PhotoCache.clear()
    }

    @After
    fun tearDown() {
        PhotoCache.clear()
    }

    @Test
    fun `getPhotos returns null when cache is empty`() {
        assertNull(PhotoCache.getPhotos())
    }

    @Test
    fun `setPhotos and getPhotos returns stored photos`() {
        PhotoCache.setPhotos(photos)
        val result = PhotoCache.getPhotos()
        assertNotNull(result)
        assertEquals(2, result?.size)
        assertEquals("1", result?.get(0)?.id)
        assertEquals("2", result?.get(1)?.id)
    }

    @Test
    fun `getPhotos returns null after clear`() {
        PhotoCache.setPhotos(photos)
        PhotoCache.clear()
        assertNull(PhotoCache.getPhotos())
    }

    @Test
    fun `setPhotos overwrites previous cache`() {
        PhotoCache.setPhotos(photos)
        val newPhotos = listOf(
            UnifiedPhotoItem(id = "3", uri = mockUri3, name = "photo3.jpg")
        )
        PhotoCache.setPhotos(newPhotos)
        val result = PhotoCache.getPhotos()
        assertEquals(1, result?.size)
        assertEquals("3", result?.get(0)?.id)
    }

    @Test
    fun `getPhotos returns null for empty list`() {
        PhotoCache.setPhotos(emptyList())
        assertNull(PhotoCache.getPhotos())
    }

    @Test
    fun `removePhotos filters out matching ids`() {
        PhotoCache.setPhotos(photos)
        PhotoCache.removePhotos(setOf("1"))
        val result = PhotoCache.getPhotos()
        assertEquals(1, result?.size)
        assertEquals("2", result?.get(0)?.id)
    }

    @Test
    fun `removePhotos with unknown id is a no-op`() {
        PhotoCache.setPhotos(photos)
        PhotoCache.removePhotos(setOf("unknown"))
        assertEquals(2, PhotoCache.getPhotos()?.size)
    }

    @Test
    fun `removePhotos with empty set is a no-op`() {
        PhotoCache.setPhotos(photos)
        PhotoCache.removePhotos(emptySet())
        assertEquals(2, PhotoCache.getPhotos()?.size)
    }

    @Test
    fun `removePhotos removing all photos returns null from getPhotos`() {
        PhotoCache.setPhotos(photos)
        PhotoCache.removePhotos(setOf("1", "2"))
        assertNull(PhotoCache.getPhotos())
    }

    @Test
    fun `getPhotos preserves photo data`() {
        PhotoCache.setPhotos(photos)
        val result = PhotoCache.getPhotos()
        val cached = result?.get(0)
        assertEquals("1", cached?.id)
        assertEquals(mockUri1, cached?.uri)
        assertEquals("photo1.jpg", cached?.name)
        assertEquals(1000L, cached?.dateModified)
        assertEquals(PhotoSource.Local, cached?.source)
        assertTrue(cached?.isOnDevice == true)
    }
}
