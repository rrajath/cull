package com.rrajath.occullt.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
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
        return "$baseUrl/api/assets/$assetId/original"
    }

    fun getOriginalUrl(assetId: String): String {
        return "$baseUrl/api/assets/$assetId/original"
    }

    suspend fun getAsset(assetId: String): Result<ImmichAsset> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/assets/$assetId"

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
                    val assetJson = Json.parseToJsonElement(body) as JsonObject
                    parseImmichAsset(assetJson)
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    suspend fun deleteAssets(
        assetIds: List<String>,
        force: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/assets"
            val idsJson = assetIds.joinToString(",", prefix = "[\"", postfix = "\"]")
            val body = """{"ids":$idsJson,"force":$force}"""

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

    suspend fun checkAssetExists(fileName: String, dateModifiedMs: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/search/metadata?originalFileName=$fileName&type=IMAGE&size=1"

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
                Result.success(false)
            } else {
                runCatching {
                    val jsonElement = Json.parseToJsonElement(body)
                    val jsonObject = jsonElement as? JsonObject
                    val assets = jsonObject?.get("assets")?.jsonArray
                    assets != null && assets.isNotEmpty()
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    data class SearchResponse(
        val assets: List<ImmichAsset>,
        val total: Int,
        val hasNextPage: Boolean,
    )

    suspend fun searchMetadata(
        createdAfter: String,
        createdBefore: String? = null,
        page: Int = 1,
        size: Int = 200,
    ): Result<SearchResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/search/metadata"

            val bodyJson = buildJsonObject {
                put("type", "IMAGE")
                put("createdAfter", createdAfter)
                createdBefore?.let { put("createdBefore", it) }
                put("page", page)
                put("size", size)
            }
            val jsonBody = bodyJson.toString()

            val request = Request.Builder()
                .url(url)
                .addHeader("x-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create("application/json".toMediaType(), jsonBody))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful) {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            } else if (body.isNullOrEmpty()) {
                Result.success(SearchResponse(emptyList(), 0, false))
            } else {
                runCatching {
                    val jsonElement = Json.parseToJsonElement(body) as JsonObject
                    val assetsGroup = jsonElement["assets"] as? JsonObject
                    val assetsArray = assetsGroup?.get("items") as? JsonArray ?: JsonArray(emptyList())
                    val total = assetsGroup?.get("total")?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val hasNextPage = assetsGroup?.get("hasNextPage")?.jsonPrimitive?.content?.toBoolean() ?: false

                    val assets = assetsArray.mapNotNull { assetJson ->
                        val asset = assetJson as? JsonObject ?: return@mapNotNull null
                        parseImmichAsset(asset)
                    }

                    SearchResponse(
                        assets = assets,
                        total = total,
                        hasNextPage = hasNextPage,
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message ?: e.javaClass.simpleName}"))
        } catch (e: Exception) {
            Result.failure(Exception("Error: ${e.message ?: e.javaClass.simpleName}"))
        }
    }

    private fun parseImmichAsset(asset: JsonObject): ImmichAsset {
        val id = asset["id"]?.jsonPrimitive?.content ?: ""
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

        return ImmichAsset(
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
