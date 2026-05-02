package com.rrajath.occullt.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImmichApi(
    baseUrl: String,
    apiKey: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = baseUrl.trimEnd('/')

    private val apiKey: String = apiKey

    private val json = Json { ignoreUnknownKeys = true }

    data class ServerInfo(
        val version: String,
        val licenseType: String?,
    )

    data class ImmichAsset(
        val id: String,
        val deviceAssetId: String,
        val fileName: String,
        val fileCreatedAt: String,
        val fileModifiedAt: String,
        val isFavorite: Boolean,
        val isTrashed: Boolean,
        val type: String,
        val thumbhash: String?,
        val exifInfo: ExifInfo?,
    )

    data class ExifInfo(
        val dateTimeOriginal: String?,
        val fileSizeInByte: Long?,
        val exifImageWidth: Int?,
        val exifImageHeight: Int?,
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

    suspend fun getAssets(
        page: Int = 1,
        size: Int = 100,
        order: String = "desc",
    ): Result<List<ImmichAsset>> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/assets?size=$size&page=$page&order=$order&withExif=true&isNotInAlbum=false"

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
                    val jsonElement = Json.parseToJsonElement(body)
                    val assetsList = when (jsonElement) {
                        is JsonArray -> jsonElement
                        is JsonObject -> jsonElement["assets"]?.jsonArray ?: JsonArray(emptyList())
                        else -> JsonArray(emptyList())
                    }

                    assetsList.mapNotNull { assetJson ->
                        val asset = assetJson as? JsonObject ?: return@mapNotNull null
                        val id = asset["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val deviceAssetId = asset["deviceAssetId"]?.jsonPrimitive?.content ?: ""
                        val fileName = asset["originalFileName"]?.jsonPrimitive?.content
                            ?: asset["fileName"]?.jsonPrimitive?.content ?: ""
                        val fileCreatedAt = asset["fileCreatedAt"]?.jsonPrimitive?.content ?: ""
                        val fileModifiedAt = asset["fileModifiedAt"]?.jsonPrimitive?.content ?: ""
                        val isFavorite = asset["isFavorite"]?.jsonPrimitive?.content?.toBoolean() ?: false
                        val isTrashed = asset["isTrashed"]?.jsonPrimitive?.content?.toBoolean() ?: false
                        val type = asset["type"]?.jsonPrimitive?.content ?: "IMAGE"
                        val thumbhash = asset["thumbhash"]?.jsonPrimitive?.content

                        val exifInfo = asset["exifInfo"]?.let { exifJson ->
                            val exifObj = exifJson as? JsonObject ?: return@let null
                            ExifInfo(
                                dateTimeOriginal = exifObj["dateTimeOriginal"]?.jsonPrimitive?.content,
                                fileSizeInByte = exifObj["fileSizeInByte"]?.jsonPrimitive?.content?.toLongOrNull(),
                                exifImageWidth = exifObj["exifImageWidth"]?.jsonPrimitive?.content?.toIntOrNull(),
                                exifImageHeight = exifObj["exifImageHeight"]?.jsonPrimitive?.content?.toIntOrNull(),
                            )
                        }

                        ImmichAsset(
                            id = id,
                            deviceAssetId = deviceAssetId,
                            fileName = fileName,
                            fileCreatedAt = fileCreatedAt,
                            fileModifiedAt = fileModifiedAt,
                            isFavorite = isFavorite,
                            isTrashed = isTrashed,
                            type = type,
                            thumbhash = thumbhash,
                            exifInfo = exifInfo,
                        )
                    }
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    suspend fun getAllAssets(): Result<List<ImmichAsset>> = withContext(Dispatchers.IO) {
        try {
            val allAssets = mutableListOf<ImmichAsset>()
            var page = 1
            val pageSize = 1000

            while (true) {
                val result = getAssets(page = page, size = pageSize)
                if (result.isFailure) {
                    return@withContext if (allAssets.isEmpty()) {
                        Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch assets"))
                    } else {
                        Result.success(allAssets)
                    }
                }

                val assets = result.getOrNull().orEmpty()
                if (assets.isEmpty()) break

                allAssets.addAll(assets)
                if (assets.size < pageSize) break

                page++
            }

            Result.success(allAssets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getThumbnailUrl(assetId: String): String {
        return "$baseUrl/api/assets/$assetId/thumbnail"
    }

    fun getPreviewUrl(assetId: String): String {
        return "$baseUrl/api/assets/$assetId/preview"
    }

    fun getOriginalUrl(assetId: String): String {
        return "$baseUrl/api/assets/$assetId/original"
    }

    suspend fun deleteAsset(assetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/assets"
            val body = """{"ids":["$assetId"]}"""

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .delete(RequestBody.create("application/json".toMediaType(), body))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    suspend fun deleteAssets(assetIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/assets"
            val idsJson = assetIds.joinToString(",", prefix = "[\"", postfix = "\"]")
            val body = """{"ids":$idsJson}"""

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .delete(RequestBody.create("application/json".toMediaType(), body))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            } else {
                Result.success(Unit)
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }
}
