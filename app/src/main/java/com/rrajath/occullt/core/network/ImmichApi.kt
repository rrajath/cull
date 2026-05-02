package com.rrajath.occullt.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ImmichApi(
    baseUrl: String,
    apiKey: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = baseUrl.trimEnd('/')

    private val apiKey: String = apiKey

    data class ServerInfo(
        val version: String,
        val licenseType: String?,
    )

    suspend fun getServerAbout(): Result<ServerInfo> = runCatching {
        val url = "$baseUrl/api/server/about"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            if (body.isNullOrEmpty()) {
                throw Exception("Empty response body")
            }

            val json = Json.parseToJsonElement(body) as? JsonObject
                ?: throw Exception("Invalid JSON response")

            val version = json["version"]?.jsonPrimitive?.content ?: "Unknown"
            val licenseType = json["licenseType"]?.jsonPrimitive?.content

            ServerInfo(version = version, licenseType = licenseType)
        }
    }
}
