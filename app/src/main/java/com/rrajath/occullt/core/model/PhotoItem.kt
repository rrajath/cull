package com.rrajath.occullt.core.model

import android.net.Uri

data class PhotoItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val dateModified: Long = 0,
)
