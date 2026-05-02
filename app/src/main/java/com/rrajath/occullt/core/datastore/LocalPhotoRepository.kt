package com.rrajath.occullt.core.datastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.rrajath.occullt.core.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalPhotoRepository(private val context: Context) {

    fun takePersistablePermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    suspend fun getPhotosFromFolder(folderUri: Uri): List<PhotoItem> = withContext(Dispatchers.IO) {
        val directory = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "bmp", "gif")

        val photos = mutableListOf<PhotoItem>()
        val files = directory.listFiles()
        files.forEach { file ->
            if (file.isFile && file.exists()) {
                val name = file.name ?: ""
                val extension = name.substringAfterLast('.', "").lowercase()
                if (extension in imageExtensions) {
                    file.uri?.let { uri ->
                        photos.add(
                            PhotoItem(
                                id = name,
                                uri = uri,
                                name = name,
                                dateModified = file.lastModified()
                            )
                        )
                    }
                }
            }
        }

        photos.sortByDescending { it.dateModified }
        photos
    }

    fun getThumbnailUri(photo: PhotoItem): Uri = photo.uri
}
