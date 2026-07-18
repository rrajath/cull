package com.rrajath.occullt.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Decides whether the Immich API key may be attached to an outgoing image request.
 *
 * The key is only attached when the request targets the configured Immich server
 * (same scheme, host, and port) on an asset endpoint — never on a mere URL
 * substring match, so a future externally-influenced URL can't exfiltrate the key.
 */
object ImmichKeyGate {

    fun shouldAttachKey(requestUrl: HttpUrl, configuredBaseUrl: String?, apiKey: String?): Boolean {
        if (apiKey.isNullOrBlank() || configuredBaseUrl.isNullOrBlank()) return false
        val base = configuredBaseUrl.trim().toHttpUrlOrNull() ?: return false
        return requestUrl.scheme == base.scheme &&
            requestUrl.host == base.host &&
            requestUrl.port == base.port &&
            requestUrl.encodedPath.contains("/api/assets/")
    }
}
