package com.rrajath.occullt.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM tests that exercise the actual HTTP requests ImmichApi sends,
 * verifying URL encoding and JSON body construction against a MockWebServer.
 */
class ImmichApiRequestTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ImmichApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ImmichApi(server.url("/").toString(), "test-key")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `checkAssetExists percent-encodes special characters in the filename`() = runTest {
        server.enqueue(MockResponse().setBody("""{"assets":[]}"""))

        api.checkAssetExists("my photo&type=VIDEO#1+2.jpg", 0L)

        val recorded = server.takeRequest()
        val url = recorded.requestUrl!!
        // The raw filename must survive round-trip decoding intact...
        assertEquals("my photo&type=VIDEO#1+2.jpg", url.queryParameter("originalFileName"))
        // ...and must not have injected extra query parameters
        assertEquals("IMAGE", url.queryParameter("type"))
        assertEquals("1", url.queryParameter("size"))
        assertEquals(3, url.queryParameterNames.size)
    }

    @Test
    fun `checkAssetExists sends api key header`() = runTest {
        server.enqueue(MockResponse().setBody("""{"assets":[]}"""))

        api.checkAssetExists("photo.jpg", 0L)

        assertEquals("test-key", server.takeRequest().getHeader("x-api-key"))
    }

    @Test
    fun `deleteAssets sends well-formed JSON body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = api.deleteAssets(listOf("id-1", "id-2"), force = false)
        assertTrue(result.isSuccess)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        val body = Json.parseToJsonElement(recorded.body.readUtf8()) as JsonObject
        assertEquals(
            listOf("id-1", "id-2"),
            body["ids"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals(false, body["force"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `deleteAssets escapes special characters in ids`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        // server-issued IDs are UUIDs in practice, but the body must stay valid
        // JSON even for hostile input
        val hostileId = """id"with\quotes"""
        api.deleteAssets(listOf(hostileId), force = true)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()) as JsonObject
        assertEquals(listOf(hostileId), body["ids"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(true, body["force"]!!.jsonPrimitive.content.toBoolean())
    }
}
