package com.rrajath.occullt.core.datastore

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
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

    suspend fun loadCameraPhotosFromMediaStore(context: Context): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%DCIM/Camera%")
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateModified = cursor.getLong(modifiedColumn)
                val uri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                photos.add(
                    PhotoItem(
                        id = id.toString(),
                        uri = uri,
                        name = name,
                        dateModified = dateModified * 1000
                    )
                )
            }
        }
        photos
    }

    fun getThumbnailUri(photo: PhotoItem): Uri = photo.uri
}
