package com.rrajath.occullt.core.datastore

import android.content.Context
import android.net.Uri
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.core.network.ImmichApi
import com.rrajath.occullt.core.network.ImmichRepository
import com.rrajath.occullt.ui.component.SourceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnifiedPhotoRepository(
    private val context: Context,
    private val immichRepository: ImmichRepository?,
) {
    private val localRepo = LocalPhotoRepository(context)

    data class PaginatedResult(
        val photos: List<UnifiedPhotoItem>,
        val hasMore: Boolean,
        val nextCreatedBefore: String?,
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
                val localPhotos = if (folderUri.isNullOrBlank() || folderUri.contains("DCIM") && folderUri.contains("Camera")) {
                    localRepo.loadCameraPhotosFromMediaStore(context).map { it.toUnified(PhotoSource.Local) }
                } else {
                    val uri = Uri.parse(folderUri)
                    localRepo.getPhotosFromFolder(uri).map { it.toUnified(PhotoSource.Local) }
                }
                return@withContext Result.success(localPhotos)
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

            val localPhotos = if (folderUri.isNullOrBlank() || (folderUri.contains("DCIM") && folderUri.contains("Camera"))) {
                localRepo.loadCameraPhotosFromMediaStore(context).map { it.toUnified(PhotoSource.Local) }
            } else {
                val uri = Uri.parse(folderUri)
                localRepo.getPhotosFromFolder(uri).map { it.toUnified(PhotoSource.Local) }
            }

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
    ): Result<PaginatedResult> = withContext(Dispatchers.IO) {
        try {
            var immichHasMore = false
            var immichNextCreatedBefore: String? = null

            val immichPhotos = if ((sourceMode == SourceMode.Immich || sourceMode == SourceMode.Hybrid) && immichRepository != null) {
                if (createdAfter == null) {
                    val sevenDaysAgo = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, -7)
                    }.time
                    val createdAfterStr = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(sevenDaysAgo)
                    val result = immichRepository.searchPhotosByDateRange(
                        createdAfter = createdAfterStr,
                        createdBefore = createdBefore,
                    )
                    val paginated = result.getOrNull()
                    immichHasMore = paginated?.hasMore ?: false
                    immichNextCreatedBefore = paginated?.nextCreatedBefore
                    paginated?.photos.orEmpty()
                } else {
                    val result = immichRepository.searchPhotosByDateRange(
                        createdAfter = createdAfter,
                        createdBefore = createdBefore,
                    )
                    val paginated = result.getOrNull()
                    immichHasMore = paginated?.hasMore ?: false
                    immichNextCreatedBefore = paginated?.nextCreatedBefore
                    paginated?.photos.orEmpty()
                }
            } else {
                emptyList()
            }

            if (sourceMode == SourceMode.Immich) {
                return@withContext Result.success(
                    PaginatedResult(
                        photos = immichPhotos,
                        hasMore = immichHasMore,
                        nextCreatedBefore = immichNextCreatedBefore,
                    )
                )
            }

            val localPhotos = if (folderUri.isNullOrBlank() || (folderUri.contains("DCIM") && folderUri.contains("Camera"))) {
                val t1 = System.currentTimeMillis()
                val result = localRepo.loadCameraPhotosFromMediaStore(context)
                android.util.Log.d("LibraryPerf", "MediaStore path took ${System.currentTimeMillis() - t1}ms")
                result.map { it.toUnified(PhotoSource.Local) }
            } else {
                val t1 = System.currentTimeMillis()
                val uri = Uri.parse(folderUri)
                val result = localRepo.getPhotosFromFolder(uri)
                android.util.Log.d("LibraryPerf", "DocumentFile path took ${System.currentTimeMillis() - t1}ms")
                result.map { it.toUnified(PhotoSource.Local) }
            }

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

private fun com.rrajath.occullt.core.model.PhotoItem.toUnified(source: PhotoSource): UnifiedPhotoItem {
    return UnifiedPhotoItem(
        id = this.id,
        uri = this.uri,
        name = this.name,
        dateModified = this.dateModified,
        source = source,
        isOnDevice = true,
        isOnImmich = false,
    )
}
