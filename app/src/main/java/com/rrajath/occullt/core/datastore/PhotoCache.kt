package com.rrajath.occullt.core.datastore

import com.rrajath.occullt.core.model.UnifiedPhotoItem

object PhotoCache {
    private var cachedPhotos: List<UnifiedPhotoItem> = emptyList()

    fun setPhotos(photos: List<UnifiedPhotoItem>) {
        cachedPhotos = photos
    }

    fun getPhotos(): List<UnifiedPhotoItem>? {
        return if (cachedPhotos.isNotEmpty()) cachedPhotos else null
    }

    fun clear() {
        cachedPhotos = emptyList()
    }
}
