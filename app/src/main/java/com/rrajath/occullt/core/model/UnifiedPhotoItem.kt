package com.rrajath.occullt.core.model

import android.net.Uri

enum class PhotoSource {
    Local,
    Immich
}

data class UnifiedPhotoItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val dateModified: Long = 0,
    val source: PhotoSource = PhotoSource.Local,
    val immichAssetId: String? = null,
    val thumbnailUrl: String? = null,
    val previewUrl: String? = null,
    val originalUrl: String? = null,
    val isOnDevice: Boolean = true,
    val isOnImmich: Boolean = false,
)
