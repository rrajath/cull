package com.rrajath.cull.core.datastore

import android.content.Context
import android.net.Uri
import com.rrajath.cull.core.model.PhotoSource
import com.rrajath.cull.core.model.UnifiedPhotoItem
import com.rrajath.cull.core.network.ImmichApi
import com.rrajath.cull.core.network.ImmichRepository
import com.rrajath.cull.ui.component.SourceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnifiedPhotoRepository(
    private val context: Context,
    private val immichRepository: ImmichRepository?,
) {
    companion object {
        private const val EPOCH_START = "1970-01-01T00:00:00.000Z"
    }

    private val localRepo = LocalPhotoRepository(context)

    // The device library doesn't change between paginated loads, so the scan
    // (MediaStore query or SAF folder listing) runs once per repository instance
    // instead of once per "Load More" page
    private var cachedLocalPhotos: List<UnifiedPhotoItem>? = null
    private var cachedLocalFolderUri: String? = null

    fun invalidateLocalCache() {
        cachedLocalPhotos = null
        cachedLocalFolderUri = null
    }

    private suspend fun loadLocalUnified(folderUri: String?): List<UnifiedPhotoItem> {
        cachedLocalPhotos?.let { cached ->
            if (cachedLocalFolderUri == folderUri) return cached
        }
        val fresh = if (folderUri.isNullOrBlank() || (folderUri.contains("DCIM") && folderUri.contains("Camera"))) {
            localRepo.loadCameraPhotosFromMediaStore(context).map { it.toUnified(PhotoSource.Local) }
        } else {
            localRepo.getPhotosFromFolder(Uri.parse(folderUri)).map { it.toUnified(PhotoSource.Local) }
        }
        cachedLocalPhotos = fresh
        cachedLocalFolderUri = folderUri
        return fresh
    }

    data class PaginatedResult(
        val photos: List<UnifiedPhotoItem>,
        val hasMore: Boolean,
        val nextCreatedBefore: String?,
        val hasNextPage: Boolean = false,
    )

    data class DeleteSummary(
        val localDeleted: Int = 0,
        val immichDeleted: Int = 0,
        val immichError: String? = null,
    )

    suspend fun loadPhotos(
        sourceMode: SourceMode,
        folderUri: String? = null,
    ): Result<List<UnifiedPhotoItem>> = withContext(Dispatchers.IO) {
        try {
            if (sourceMode == SourceMode.Local) {
                return@withContext Result.success(loadLocalUnified(folderUri))
            }

            if (sourceMode == SourceMode.Immich) {
                val immichPhotos = when {
                    immichRepository == null -> emptyList()
                    else -> {
                        val result = immichRepository.getAllPhotos()
                        result.getOrNull().orEmpty()
                    }
                }
                return@withContext Result.success(immichPhotos)
            }

            val localPhotos = loadLocalUnified(folderUri)

            val immichPhotos = when {
                immichRepository == null -> emptyList()
                else -> {
                    val result = immichRepository.getAllPhotos()
                    result.getOrNull().orEmpty()
                }
            }

            val hybridPhotos = intersectPhotoLists(localPhotos, immichPhotos)

            Result.success(hybridPhotos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadPhotosPaginated(
        sourceMode: SourceMode,
        folderUri: String? = null,
        createdAfter: String? = null,
        createdBefore: String? = null,
        page: Int = 1,
        size: Int = 1000,
    ): Result<PaginatedResult> = withContext(Dispatchers.IO) {
        try {
            var immichHasMore = false
            var immichNextCreatedBefore: String? = null
            var immichHasNextPage = false

            val immichPhotos = if ((sourceMode == SourceMode.Immich || sourceMode == SourceMode.Hybrid) && immichRepository != null) {
                // No createdAfter means the whole library — page from the epoch
                // via /api/search/metadata rather than a rolling date window
                val result = immichRepository.searchPhotosByDateRange(
                    createdAfter = createdAfter ?: EPOCH_START,
                    createdBefore = createdBefore,
                    page = page,
                    size = size,
                )
                val paginated = result.getOrNull()
                immichHasMore = paginated?.hasMore ?: false
                immichNextCreatedBefore = paginated?.nextCreatedBefore
                immichHasNextPage = paginated?.hasNextPage ?: false
                paginated?.photos.orEmpty()
            } else {
                emptyList()
            }

            if (sourceMode == SourceMode.Immich) {
                return@withContext Result.success(
                    PaginatedResult(
                        photos = immichPhotos,
                        hasMore = immichHasMore,
                        nextCreatedBefore = immichNextCreatedBefore,
                        hasNextPage = immichHasNextPage,
                    )
                )
            }

            val localPhotos = loadLocalUnified(folderUri)

            val hybridPhotos = if (sourceMode == SourceMode.Hybrid) {
                intersectPhotoLists(localPhotos, immichPhotos)
            } else {
                localPhotos
            }

            Result.success(
                PaginatedResult(
                    photos = hybridPhotos,
                    hasMore = immichHasMore,
                    nextCreatedBefore = immichNextCreatedBefore,
                    hasNextPage = immichHasNextPage,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun intersectPhotoLists(
        local: List<UnifiedPhotoItem>,
        immich: List<UnifiedPhotoItem>,
    ): List<UnifiedPhotoItem> {
        val immichByName = immich.associateBy { it.name }
        val result = mutableListOf<UnifiedPhotoItem>()

        for (localPhoto in local) {
            val matchingImmich = immichByName[localPhoto.name]
            if (matchingImmich != null) {
                // Hybrid items keep the local photo's timestamps (incl. dateTaken)
                result.add(
                    localPhoto.copy(
                        isOnImmich = true,
                        immichAssetId = matchingImmich.immichAssetId,
                        thumbnailUrl = matchingImmich.thumbnailUrl,
                        previewUrl = matchingImmich.previewUrl,
                        originalUrl = matchingImmich.originalUrl,
                    )
                )
            }
        }

        return result.sortedByDescending { it.dateModified }
    }

    suspend fun deletePhotos(
        photos: List<UnifiedPhotoItem>,
        mirrorDeletes: Boolean = false,
    ): Result<DeleteSummary> = withContext(Dispatchers.IO) {
        try {
            val localPhotos = photos.filter { it.isOnDevice }

            if (localPhotos.isNotEmpty()) {
                val urisToDelete = localPhotos.map { it.uri }
                deletePhotosViaMediaStore(urisToDelete)
            }

            var immichDeleted = 0
            var immichError: String? = null

            if (immichRepository != null) {
                val immichIds = photos.mapNotNull {
                    if (it.isOnImmich && it.immichAssetId != null) it.immichAssetId else null
                }
                if (immichIds.isNotEmpty()) {
                    val result = immichRepository.deletePhotos(immichIds)
                    if (result.isSuccess) {
                        immichDeleted = immichIds.size
                    } else {
                        immichError = result.exceptionOrNull()?.message ?: "Unknown error"
                    }
                }
            }

            Result.success(DeleteSummary(
                localDeleted = localPhotos.size,
                immichDeleted = immichDeleted,
                immichError = immichError,
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deletePhotosViaMediaStore(uris: List<Uri>) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val pendingIntent = android.provider.MediaStore.createTrashRequest(context.contentResolver, uris, true)
                pendingIntent.send()
            } catch (e: Exception) {
                try {
                    val pendingIntent = android.provider.MediaStore.createDeleteRequest(context.contentResolver, uris)
                    pendingIntent.send()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        } else {
            uris.forEach { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

private fun com.rrajath.cull.core.model.PhotoItem.toUnified(source: PhotoSource): UnifiedPhotoItem {
    return UnifiedPhotoItem(
        id = this.id,
        uri = this.uri,
        name = this.name,
        dateModified = this.dateModified,
        dateTaken = this.dateTaken,
        source = source,
        isOnDevice = true,
        isOnImmich = false,
    )
}
