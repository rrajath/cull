package com.rrajath.occullt.core.network

import android.net.Uri
import com.rrajath.occullt.core.database.ImmichAssetMapping
import com.rrajath.occullt.core.database.ImmichAssetMappingDb
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ImmichRepository(
    private val immichApi: ImmichApi,
    private val mappingDb: ImmichAssetMappingDb? = null,
) {
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

                    UnifiedPhotoItem(
                        id = "immich_${asset.id}",
                        uri = Uri.parse(immichApi.getPreviewUrl(asset.id)),
                        name = asset.fileName,
                        dateModified = dateModified,
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

    suspend fun getAllPhotos(): Result<List<UnifiedPhotoItem>> = withContext(Dispatchers.IO) {
        try {
            val result = immichApi.getAllAssets()
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch assets"))
            }

            val assets = result.getOrNull().orEmpty()
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

                    UnifiedPhotoItem(
                        id = "immich_${asset.id}",
                        uri = Uri.parse(immichApi.getPreviewUrl(asset.id)),
                        name = asset.fileName,
                        dateModified = dateModified,
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
