package com.rrajath.occullt.core.network

import android.net.Uri
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ImmichRepository(
    private val immichApi: ImmichApi,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    suspend fun getAllPhotos(): Result<List<UnifiedPhotoItem>> = withContext(Dispatchers.IO) {
        try {
            val result = immichApi.getAllAssets()
            if (result.isFailure) {
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch assets"))
            }

            val assets = result.getOrNull().orEmpty()
            val photos = assets
                .filter { it.type == "IMAGE" && !it.isTrashed }
                .map { asset ->
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

            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhotos(assetIds: List<String>): Result<Unit> {
        val immichIds = assetIds.mapNotNull { id ->
            if (id.startsWith("immich_")) id.removePrefix("immich_") else null
        }

        if (immichIds.isEmpty()) {
            return Result.success(Unit)
        }

        return immichApi.deleteAssets(immichIds)
    }

    fun getThumbnailUrl(photo: UnifiedPhotoItem): String? {
        return if (photo.source == PhotoSource.Immich) photo.thumbnailUrl else null
    }
}
