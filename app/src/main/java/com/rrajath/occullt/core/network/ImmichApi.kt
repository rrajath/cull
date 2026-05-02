package com.rrajath.occullt.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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

    suspend fun getServerAbout(): Result<ServerInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/server/about"

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            } else if (body.isNullOrEmpty()) {
                Result.failure(Exception("Empty response body"))
            } else {
                runCatching {
                    val json = Json.parseToJsonElement(body) as JsonObject
                    val version = json["version"]?.jsonPrimitive?.content ?: "Unknown"
                    val licenseType = json["licenseType"]?.jsonPrimitive?.content
                    ServerInfo(version = version, licenseType = licenseType)
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }
}
