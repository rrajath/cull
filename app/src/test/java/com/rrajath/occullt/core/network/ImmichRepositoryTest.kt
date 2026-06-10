package com.rrajath.occullt.core.network

import android.net.Uri
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImmichRepositoryTest {

    private val mockUri = mockk<Uri>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockUri
    }

    @Test
    fun `getThumbnailUrl returns thumbnailUrl for Immich source`() {
        val api = ImmichApi("https://fake.com", "key")
        val repo = ImmichRepository(api)
        val photo = UnifiedPhotoItem(
            id = "1", uri = mockUri, name = "test.jpg",
            source = PhotoSource.Immich, thumbnailUrl = "https://example.com/thumb.jpg"
        )
        val result = repo.getThumbnailUrl(photo)
        assertEquals("https://example.com/thumb.jpg", result)
    }

    @Test
    fun `getThumbnailUrl returns null for Local source`() {
        val api = ImmichApi("https://fake.com", "key")
        val repo = ImmichRepository(api)
        val photo = UnifiedPhotoItem(
            id = "1", uri = mockUri, name = "test.jpg",
            source = PhotoSource.Local, thumbnailUrl = "https://example.com/thumb.jpg"
        )
        val result = repo.getThumbnailUrl(photo)
        assertNull(result)
    }

    @Test
    fun `deletePhotos with empty list returns success`() = runTest {
        val repo = ImmichRepository(mockk(relaxed = true))
        val result = repo.deletePhotos(emptyList())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deletePhotos with blank ids returns success`() = runTest {
        val repo = ImmichRepository(mockk(relaxed = true))
        val result = repo.deletePhotos(listOf("", "  "))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `deletePhotos with valid ids calls api deleteAssets`() = runTest {
        val api = mockk<ImmichApi>(relaxUnitFun = true)
        coEvery { api.deleteAssets(listOf("id1", "id2")) } returns Result.success(Unit)
        val repo = ImmichRepository(api)
        val result = repo.deletePhotos(listOf("id1", "id2"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getAllPhotos returns failure when api fails`() = runTest {
        val api = mockk<ImmichApi>(relaxUnitFun = true)
        every { api.getThumbnailUrl(any()) } returns ""
        every { api.getPreviewUrl(any()) } returns ""
        every { api.getOriginalUrl(any()) } returns ""
        coEvery { api.getAllAssets() } returns Result.failure(Exception("API error"))

        val repo = ImmichRepository(api)
        val result = repo.getAllPhotos()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAllPhotos filters out non-image and trashed assets`() = runTest {
        val api = mockk<ImmichApi>(relaxUnitFun = true)
        every { api.getThumbnailUrl(any()) } returns "https://example.com/thumb/"
        every { api.getPreviewUrl(any()) } returns "https://example.com/preview/"
        every { api.getOriginalUrl(any()) } returns "https://example.com/original/"

        val assets = listOf(
            ImmichApi.ImmichAsset(
                id = "1", deviceAssetId = "d1", fileName = "photo1",
                fileCreatedAt = "2024-01-01T00:00:00.000Z",
                fileModifiedAt = "2024-01-01T00:00:00.000Z",
                isFavorite = false, isTrashed = false, type = "IMAGE",
                thumbhash = null, exifInfo = null,
            ),
            ImmichApi.ImmichAsset(
                id = "2", deviceAssetId = "d2", fileName = "video1",
                fileCreatedAt = "2024-01-01T00:00:00.000Z",
                fileModifiedAt = "2024-01-01T00:00:00.000Z",
                isFavorite = false, isTrashed = false, type = "VIDEO",
                thumbhash = null, exifInfo = null,
            ),
            ImmichApi.ImmichAsset(
                id = "3", deviceAssetId = "d3", fileName = "trashed1",
                fileCreatedAt = "2024-01-01T00:00:00.000Z",
                fileModifiedAt = "2024-01-01T00:00:00.000Z",
                isFavorite = false, isTrashed = true, type = "IMAGE",
                thumbhash = null, exifInfo = null,
            ),
        )
        coEvery { api.getAllAssets() } returns Result.success(assets)

        val repo = ImmichRepository(api)
        val result = repo.getAllPhotos()
        assertTrue("getAllPhotos failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val photos = result.getOrNull()
        assertEquals(1, photos?.size)
        assertEquals("immich_1", photos?.get(0)?.id)
        assertEquals("photo1", photos?.get(0)?.name)
    }

    @Test
    fun `searchPhotosByDateRange returns paginated results`() = runTest {
        val api = mockk<ImmichApi>(relaxUnitFun = true)
        every { api.getThumbnailUrl(any()) } returns "https://example.com/thumb/"
        every { api.getPreviewUrl(any()) } returns "https://example.com/preview/"
        every { api.getOriginalUrl(any()) } returns "https://example.com/original/"

        val asset = ImmichApi.ImmichAsset(
            id = "1", deviceAssetId = "d1", fileName = "photo1",
            fileCreatedAt = "2024-01-01T00:00:00.000Z",
            fileModifiedAt = "2024-01-01T00:00:00.000Z",
            isFavorite = false, isTrashed = false, type = "IMAGE",
            thumbhash = null, exifInfo = null,
        )
        coEvery {
            api.searchMetadata(
                createdAfter = "2024-01-01T00:00:00.000Z",
                createdBefore = null,
                page = 1,
                size = 200,
            )
        } returns Result.success(
            ImmichApi.SearchResponse(
                assets = listOf(asset),
                total = 1,
                hasNextPage = false,
            )
        )

        val repo = ImmichRepository(api)
        val result = repo.searchPhotosByDateRange(createdAfter = "2024-01-01T00:00:00.000Z")
        assertTrue("search failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val paginated = result.getOrNull()
        assertTrue(paginated?.photos?.isNotEmpty() == true)
        assertEquals(1, paginated?.photos?.size)
        assertEquals("immich_1", paginated?.photos?.get(0)?.id)
    }
}
