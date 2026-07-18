package com.rrajath.occullt.core.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmichKeyGateTest {

    private val base = "https://immich.example.com"
    private val key = "secret-key"

    @Test
    fun `attaches key for asset request on configured host`() {
        val url = "https://immich.example.com/api/assets/abc123/thumbnail".toHttpUrl()
        assertTrue(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `attaches key when base url has trailing slash and port`() {
        val url = "https://immich.example.com:2283/api/assets/abc123/original".toHttpUrl()
        assertTrue(ImmichKeyGate.shouldAttachKey(url, "https://immich.example.com:2283/", key))
    }

    @Test
    fun `does not attach key to a different host`() {
        val url = "https://evil.example.org/api/assets/abc123/thumbnail".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `does not attach key when path merely contains api assets on foreign host`() {
        val url = "https://attacker.com/steal/api/assets/x".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `does not attach key on scheme downgrade`() {
        val url = "http://immich.example.com/api/assets/abc123/thumbnail".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `does not attach key on different port`() {
        val url = "https://immich.example.com:8443/api/assets/abc123/thumbnail".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `does not attach key for non-asset path on configured host`() {
        val url = "https://immich.example.com/api/server/about".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, key))
    }

    @Test
    fun `does not attach key when key or base url missing`() {
        val url = "https://immich.example.com/api/assets/abc123/thumbnail".toHttpUrl()
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, null))
        assertFalse(ImmichKeyGate.shouldAttachKey(url, base, " "))
        assertFalse(ImmichKeyGate.shouldAttachKey(url, null, key))
        assertFalse(ImmichKeyGate.shouldAttachKey(url, "not a url", key))
    }
}
