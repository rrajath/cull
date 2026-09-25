package com.rrajath.cull.core.network

import android.net.Uri
import com.rrajath.cull.core.database.ImmichAssetMapping
import com.rrajath.cull.core.database.ImmichAssetMappingDb
import com.rrajath.cull.core.model.PhotoSource
import com.rrajath.cull.core.model.UnifiedPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Parses the first parseable ISO-8601 timestamp among [candidates] to epoch ms.
 * Immich sends both offset-bearing ("2024-01-15T10:30:00.000+00:00") and
 * Z-suffixed strings depending on the field. Returns 0 if nothing parses.
 */
fun parseImmichTimestamp(vararg candidates: String?): Long {
    val legacyFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    for (candidate in candidates) {
        if (candidate.isNullOrBlank()) continue
        runCatching { return OffsetDateTime.parse(candidate).toInstant().toEpochMilli() }
        runCatching { return Instant.parse(candidate).toEpochMilli() }
        runCatching { legacyFormat.parse(candidate)?.let { return it.time } }
    }
    return 0L
}

class ImmichRepository(
    private val immichApi: ImmichApi,
    private val mappingDb: ImmichAssetMappingDb? = null,
) {
    companion object {
        private const val EPOCH_START = "1970-01-01T00:00:00.000Z"
        private const val FULL_SCAN_PAGE_SIZE = 1000
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    data class PaginatedResult(
        val photos: List<UnifiedPhotoItem>,
        val hasMore: Boolean,
        val nextCreatedBefore: String?,
        val hasNextPage: Boolean = false,
    )

    suspend fun searchPhotosByDateRange(
        createdAfter: String,
        createdBefore: String? = null,
        page: Int = 1,
        size: Int = 200,
    ): Result<PaginatedResult> = withContext(Dispatchers.IO) {
        try {
            val result = immichApi.searchMetadata(
                createdAfter = createdAfter,
                createdBefore = createdBefore,
                page = page,
                size = size,
            )

            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch assets"))
            }

            val searchResponse = result.getOrThrow()
            val mappings = mutableListOf<ImmichAssetMapping>()
            val photos = searchResponse.assets
                .filter { it.type == "IMAGE" && !it.isTrashed }
                .map { asset ->
                    mappings.add(
                        ImmichAssetMapping(
                            id = asset.id,
                            originalFileName = asset.fileName,
                            fileCreatedAt = asset.fileCreatedAt,
                            fileModifiedAt = asset.fileModifiedAt,
                        )
                    )
                    val dateModified = runCatching {
                        dateFormat.parse(asset.fileModifiedAt)?.time ?: 0L
                    }.getOrDefault(0L)
                    val dateTaken = parseImmichTimestamp(
                        asset.exifInfo?.dateTimeOriginal,
                        asset.fileCreatedAt,
                        asset.fileModifiedAt,
                    ).takeIf { it > 0L } ?: dateModified

                    UnifiedPhotoItem(
                        id = "immich_${asset.id}",
                        uri = Uri.parse(immichApi.getPreviewUrl(asset.id)),
                        name = asset.fileName,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        source = PhotoSource.Immich,
                        immichAssetId = asset.id,
                        thumbnailUrl = immichApi.getThumbnailUrl(asset.id),
                        previewUrl = immichApi.getPreviewUrl(asset.id),
                        originalUrl = immichApi.getOriginalUrl(asset.id),
                        isOnDevice = false,
                        isOnImmich = true,
                    )
                }
                .sortedByDescending { it.dateModified }

            if (mappings.isNotEmpty()) {
                mappingDb?.insertAll(mappings)
            }

            val nextCreatedBefore = if (photos.isNotEmpty()) {
                val oldestDate = photos.last().dateModified
                dateFormat.format(java.util.Date(oldestDate))
            } else {
                null
            }

            Result.success(
                PaginatedResult(
                    photos = photos,
                    hasMore = photos.isNotEmpty(),
                    nextCreatedBefore = nextCreatedBefore,
                    hasNextPage = searchResponse.hasNextPage,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the entire library by paging POST /api/search/metadata — the
     * same endpoint the Library view uses. (GET /api/assets, which the old
     * implementation paged, has been removed from recent Immich servers and
     * returns nothing.)
     */
    suspend fun getAllPhotos(): Result<List<UnifiedPhotoItem>> = withContext(Dispatchers.IO) {
        try {
            val assets = mutableListOf<ImmichApi.ImmichAsset>()
            var page = 1
            while (true) {
                val result = immichApi.searchMetadata(
                    createdAfter = EPOCH_START,
                    createdBefore = null,
                    page = page,
                    size = FULL_SCAN_PAGE_SIZE,
                )
                if (result.isFailure) {
                    return@withContext Result.failure(
                        result.exceptionOrNull() ?: Exception("Failed to fetch assets")
                    )
                }
                val response = result.getOrThrow()
                assets.addAll(response.assets)
                if (!response.hasNextPage || response.assets.isEmpty()) break
                page++
            }
            val mappings = mutableListOf<ImmichAssetMapping>()
            val photos = assets
                .filter { it.type == "IMAGE" && !it.isTrashed }
                .map { asset ->
                    mappings.add(
                        ImmichAssetMapping(
                            id = asset.id,
                            originalFileName = asset.fileName,
                            fileCreatedAt = asset.fileCreatedAt,
                            fileModifiedAt = asset.fileModifiedAt,
                        )
                    )
                    val dateModified = runCatching {
                        dateFormat.parse(asset.fileModifiedAt)?.time ?: 0L
                    }.getOrDefault(0L)
                    val dateTaken = parseImmichTimestamp(
                        asset.exifInfo?.dateTimeOriginal,
                        asset.fileCreatedAt,
                        asset.fileModifiedAt,
                    ).takeIf { it > 0L } ?: dateModified

                    UnifiedPhotoItem(
                        id = "immich_${asset.id}",
                        uri = Uri.parse(immichApi.getPreviewUrl(asset.id)),
                        name = asset.fileName,
                        dateModified = dateModified,
                        dateTaken = dateTaken,
                        source = PhotoSource.Immich,
                        immichAssetId = asset.id,
                        thumbnailUrl = immichApi.getThumbnailUrl(asset.id),
                        previewUrl = immichApi.getPreviewUrl(asset.id),
                        originalUrl = immichApi.getOriginalUrl(asset.id),
                        isOnDevice = false,
                        isOnImmich = true,
                    )
                }
                .sortedByDescending { it.dateModified }

            if (mappings.isNotEmpty()) {
                mappingDb?.insertAll(mappings)
            }

            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhotos(assetIds: List<String>): Result<Unit> {
        val immichIds = assetIds.filter { it.isNotBlank() }

        if (immichIds.isEmpty()) {
            return Result.success(Unit)
        }

        return immichApi.deleteAssets(immichIds)
    }

    fun getThumbnailUrl(photo: UnifiedPhotoItem): String? {
        return if (photo.source == PhotoSource.Immich) photo.thumbnailUrl else null
    }
}
