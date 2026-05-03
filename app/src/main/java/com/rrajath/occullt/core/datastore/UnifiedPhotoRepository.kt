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

    suspend fun loadPhotos(
        sourceMode: SourceMode,
        folderUri: String? = null,
    ): Result<List<UnifiedPhotoItem>> = withContext(Dispatchers.IO) {
        try {
            if (sourceMode == SourceMode.Local) {
                val localPhotos = when {
                    folderUri == "mediastore" -> {
                        localRepo.loadCameraPhotosFromMediaStore(context).map { it.toUnified(PhotoSource.Local) }
                    }
                    folderUri != null -> {
                        val uri = Uri.parse(folderUri)
                        localRepo.getPhotosFromFolder(uri).map { it.toUnified(PhotoSource.Local) }
                    }
                    else -> emptyList()
                }
                return@withContext Result.success(localPhotos)
            }

            val localPhotos = when {
                folderUri == "mediastore" -> {
                    localRepo.loadCameraPhotosFromMediaStore(context).map { it.toUnified(PhotoSource.Local) }
                }
                folderUri != null -> {
                    val uri = Uri.parse(folderUri)
                    localRepo.getPhotosFromFolder(uri).map { it.toUnified(PhotoSource.Local) }
                }
                else -> emptyList()
            }

            val immichPhotos = when {
                immichRepository == null -> emptyList()
                else -> {
                    val result = immichRepository.getAllPhotos()
                    result.getOrNull().orEmpty()
                }
            }

            val merged = when (sourceMode) {
                SourceMode.Immich -> immichPhotos
                SourceMode.Hybrid -> mergePhotoLists(localPhotos, immichPhotos)
                SourceMode.Local -> localPhotos
            }

            Result.success(merged)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mergePhotoLists(
        local: List<UnifiedPhotoItem>,
        immich: List<UnifiedPhotoItem>,
    ): List<UnifiedPhotoItem> {
        val merged = mutableListOf<UnifiedPhotoItem>()
        val allPhotos = local + immich
        merged.addAll(allPhotos.sortedByDescending { it.dateModified })
        return merged
    }

    suspend fun deletePhotos(
        photos: List<UnifiedPhotoItem>,
        mirrorDeletes: Boolean = false,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val localPhotos = photos.filter { it.source == PhotoSource.Local && it.isOnDevice }
            val immichPhotos = photos.filter { it.source == PhotoSource.Immich && it.isOnImmich }

            if (localPhotos.isNotEmpty()) {
                val urisToDelete = localPhotos.map { it.uri }
                deletePhotosViaMediaStore(urisToDelete)
            }

            if (immichPhotos.isNotEmpty() && mirrorDeletes && immichRepository != null) {
                val immichIds = immichPhotos.mapNotNull { it.immichAssetId }
                immichRepository.deletePhotos(immichIds)
            }

            Result.success(Unit)
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
