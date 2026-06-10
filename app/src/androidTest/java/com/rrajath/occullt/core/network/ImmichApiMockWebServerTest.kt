package com.rrajath.occullt.core.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImmichApiMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ImmichApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ImmichApi(server.url("").toString().trimEnd('/'), "test-api-key")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getServerAbout_parsesVersionAndLicense() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "version": "v1.118.0",
                    "licenseType": "free"
                }
            """.trimIndent())
        )

        val result = api.getServerAbout()
        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertEquals("v1.118.0", info?.version)
        assertEquals("free", info?.licenseType)
    }

    @Test
    fun getServerAbout_handlesMissingLicenseType() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "version": "v1.118.0"
                }
            """.trimIndent())
        )

        val result = api.getServerAbout()
        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertEquals("v1.118.0", info?.version)
        assertNull(info?.licenseType)
    }

    @Test
    fun getServerAbout_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = api.getServerAbout()
        assertTrue(result.isFailure)
    }

    @Test
    fun getAssets_parsesArrayResponse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                [
                    {
                        "id": "asset_1",
                        "deviceAssetId": "device-1",
                        "originalFileName": "photo1.jpg",
                        "fileCreatedAt": "2024-01-01T00:00:00.000Z",
                        "fileModifiedAt": "2024-01-01T00:00:00.000Z",
                        "isFavorite": false,
                        "isTrashed": false,
                        "type": "IMAGE",
                        "thumbhash": "hash123"
                    },
                    {
                        "id": "asset_2",
                        "deviceAssetId": "device-2",
                        "originalFileName": "photo2.jpg",
                        "fileCreatedAt": "2024-01-02T00:00:00.000Z",
                        "fileModifiedAt": "2024-01-02T00:00:00.000Z",
                        "isFavorite": true,
                        "isTrashed": false,
                        "type": "IMAGE",
                        "thumbhash": "hash456",
                        "exifInfo": {
                            "dateTimeOriginal": "2024-01-02T00:00:00",
                            "fileSizeInByte": 2048576,
                            "exifImageWidth": 4000,
                            "exifImageHeight": 3000
                        }
                    }
                ]
            """.trimIndent())
        )

        val result = api.getAssets(page = 1, size = 100)
        assertTrue(result.isSuccess)
        val assets = result.getOrNull()
        assertNotNull(assets)
        assertEquals(2, assets?.size)

        val first = assets?.get(0)
        assertEquals("asset_1", first?.id)
        assertEquals("photo1.jpg", first?.fileName)
        assertEquals("IMAGE", first?.type)
        assertFalse(first?.isFavorite == true)
        assertFalse(first?.isTrashed == true)

        val second = assets?.get(1)
        assertEquals("asset_2", second?.id)
        assertEquals("photo2.jpg", second?.fileName)
        assertTrue(second?.isFavorite == true)
        assertNotNull(second?.exifInfo)
        assertEquals(4000, second?.exifInfo?.exifImageWidth)
    }

    @Test
    fun getAssets_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = api.getAssets()
        assertTrue(result.isFailure)
    }

    @Test
    fun getAsset_parsesSingleAsset() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "id": "asset_1",
                    "deviceAssetId": "device-1",
                    "originalFileName": "photo1.jpg",
                    "fileCreatedAt": "2024-01-01T00:00:00.000Z",
                    "fileModifiedAt": "2024-01-01T00:00:00.000Z",
                    "isFavorite": false,
                    "isTrashed": false,
                    "type": "IMAGE",
                    "thumbhash": "hash123",
                    "exifInfo": {
                        "dateTimeOriginal": "2024-01-01T00:00:00",
                        "exifImageWidth": 4000
                    }
                }
            """.trimIndent())
        )

        val result = api.getAsset("asset_1")
        assertTrue(result.isSuccess)
        val asset = result.getOrNull()
        assertEquals("asset_1", asset?.id)
        assertEquals("photo1.jpg", asset?.fileName)
        assertEquals("IMAGE", asset?.type)
        assertEquals(4000, asset?.exifInfo?.exifImageWidth)
    }

    @Test
    fun deleteAssets_sendsCorrectRequest() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(204)
        )

        val result = api.deleteAssets(listOf("asset_1", "asset_2"))
        assertTrue(result.isSuccess)

        val recordedRequest = server.takeRequest()
        assertEquals("DELETE", recordedRequest.method)
        assertTrue(recordedRequest.body.readUtf8().contains("\"ids\""))
    }

    @Test
    fun deleteAssets_returnsFailureOnHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))

        val result = api.deleteAssets(listOf("asset_1"))
        assertTrue(result.isFailure)
    }

    @Test
    fun searchMetadata_parsesPaginatedResponse() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "assets": {
                        "items": [
                            {
                                "id": "asset_1",
                                "deviceAssetId": "device-1",
                                "originalFileName": "photo1.jpg",
                                "fileCreatedAt": "2024-01-01T00:00:00.000Z",
                                "fileModifiedAt": "2024-01-01T00:00:00.000Z",
                                "isFavorite": false,
                                "isTrashed": false,
                                "type": "IMAGE"
                            }
                        ],
                        "total": 1,
                        "hasNextPage": false
                    }
                }
            """.trimIndent())
        )

        val result = api.searchMetadata(createdAfter = "2024-01-01T00:00:00.000Z")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertNotNull(response)
        assertEquals(1, response?.total)
        assertEquals(1, response?.assets?.size)
        assertFalse(response?.hasNextPage == true)
        assertEquals("photo1.jpg", response?.assets?.get(0)?.fileName)
    }

    @Test
    fun searchMetadata_handlesEmptyResults() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "assets": {
                        "items": [],
                        "total": 0,
                        "hasNextPage": false
                    }
                }
            """.trimIndent())
        )

        val result = api.searchMetadata(createdAfter = "2024-01-01T00:00:00.000Z")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals(0, response?.total)
        assertTrue(response?.assets?.isEmpty() == true)
    }

    @Test
    fun checkAssetExists_returnsTrueWhenFound() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "assets": [
                        {
                            "id": "asset_1",
                            "deviceAssetId": "device-1",
                            "originalFileName": "photo1.jpg",
                            "fileCreatedAt": "2024-01-01T00:00:00.000Z",
                            "fileModifiedAt": "2024-01-01T00:00:00.000Z",
                            "isFavorite": false,
                            "isTrashed": false,
                            "type": "IMAGE"
                        }
                    ]
                }
            """.trimIndent())
        )

        val result = api.checkAssetExists("photo1.jpg", 1000L)
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun checkAssetExists_returnsFalseWhenNotFound() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""
                {
                    "assets": []
                }
            """.trimIndent())
        )

        val result = api.checkAssetExists("nonexistent.jpg", 1000L)
        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrNull())
    }

    @Test
    fun getAllAssets_paginatesCorrectly() = runBlocking {
        // First page returns full page
        val page1Assets = (1..1000).map { i ->
            """
                {
                    "id": "asset_$i",
                    "deviceAssetId": "device-$i",
                    "originalFileName": "photo$i.jpg",
                    "fileCreatedAt": "2024-01-01T00:00:00.000Z",
                    "fileModifiedAt": "2024-01-01T00:00:00.000Z",
                    "isFavorite": false,
                    "isTrashed": false,
                    "type": "IMAGE"
                }
            """.trimIndent()
        }
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("[${page1Assets.joinToString(",")}]")
        )

        // Second page returns empty (end of list)
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("[]")
        )

        val result = api.getAllAssets()
        assertTrue(result.isSuccess)
        val assets = result.getOrNull()
        assertEquals(1000, assets?.size)
    }
}
