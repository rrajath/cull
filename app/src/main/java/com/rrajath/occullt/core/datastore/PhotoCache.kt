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

    fun removePhotos(ids: Set<String>) {
        if (ids.isEmpty()) return
        cachedPhotos = cachedPhotos.filterNot { ids.contains(it.id) }
    }

    fun clear() {
        cachedPhotos = emptyList()
    }
}
