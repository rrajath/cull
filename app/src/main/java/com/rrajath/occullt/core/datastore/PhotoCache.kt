package com.rrajath.occullt.core.datastore

import com.rrajath.occullt.core.model.UnifiedPhotoItem

object PhotoCache {
    private var cachedPhotos: List<UnifiedPhotoItem> = emptyList()
    private var cachedFolderUri: String? = null

    fun setPhotos(photos: List<UnifiedPhotoItem>, folderUri: String?) {
        cachedPhotos = photos
        cachedFolderUri = folderUri
    }

    fun getPhotos(folderUri: String?): List<UnifiedPhotoItem>? {
        return if (cachedFolderUri == folderUri && cachedPhotos.isNotEmpty()) {
            cachedPhotos
        } else {
            null
        }
    }

    fun clear() {
        cachedPhotos = emptyList()
        cachedFolderUri = null
    }
}
