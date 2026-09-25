package com.rrajath.cull.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ImmichApiTest {

    @Test
    fun `getThumbnailUrl constructs correct URL`() {
        val api = ImmichApi("https://immich.example.com", "test-key")
        val url = api.getThumbnailUrl("asset_123")
        assertEquals("https://immich.example.com/api/assets/asset_123/thumbnail", url)
    }

    @Test
    fun `getPreviewUrl constructs correct URL`() {
        val api = ImmichApi("https://immich.example.com", "test-key")
        val url = api.getPreviewUrl("asset_123")
        assertEquals("https://immich.example.com/api/assets/asset_123/thumbnail?size=preview", url)
    }

    @Test
    fun `getOriginalUrl constructs correct URL`() {
        val api = ImmichApi("https://immich.example.com", "test-key")
        val url = api.getOriginalUrl("asset_123")
        assertEquals("https://immich.example.com/api/assets/asset_123/original", url)
    }

    @Test
    fun `trailing slash in base URL is trimmed`() {
        val api = ImmichApi("https://immich.example.com/", "test-key")
        val url = api.getThumbnailUrl("asset_1")
        assertEquals("https://immich.example.com/api/assets/asset_1/thumbnail", url)
    }

    @Test
    fun `double trailing slash in base URL is trimmed`() {
        val api = ImmichApi("https://immich.example.com//", "test-key")
        val url = api.getThumbnailUrl("asset_1")
        assertEquals("https://immich.example.com/api/assets/asset_1/thumbnail", url)
    }

    @Test
    fun `getThumbnailUrl with different asset IDs returns different URLs`() {
        val api = ImmichApi("https://immich.example.com", "test-key")
        val url1 = api.getThumbnailUrl("asset_a")
        val url2 = api.getThumbnailUrl("asset_b")
        assertEquals("https://immich.example.com/api/assets/asset_a/thumbnail", url1)
        assertEquals("https://immich.example.com/api/assets/asset_b/thumbnail", url2)
    }

    @Test
    fun `getPreviewUrl returns the server-resized preview, not the original`() {
        val api = ImmichApi("https://immich.example.com", "test-key")
        assertNotEquals(api.getPreviewUrl("asset_1"), api.getOriginalUrl("asset_1"))
    }
}
