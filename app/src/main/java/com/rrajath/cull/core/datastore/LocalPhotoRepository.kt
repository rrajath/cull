package com.rrajath.cull.core.datastore

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.rrajath.cull.core.model.PhotoItem
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
        // One DocumentsContract children query instead of DocumentFile.listFiles()
        // plus per-file isFile/name/lastModified calls — each of those is a separate
        // Binder IPC, which made large folders take seconds to list
        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId)
        val photos = mutableListOf<PhotoItem>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeColumn) ?: continue
                if (!mimeType.startsWith("image/")) continue
                val name = cursor.getString(nameColumn) ?: ""
                // SAF gives no cheap EXIF access; lastModified is the date-taken fallback
                val lastModified = cursor.getLong(modifiedColumn)
                val uri = DocumentsContract.buildDocumentUriUsingTree(folderUri, cursor.getString(idColumn))
                photos.add(
                    PhotoItem(
                        id = name,
                        uri = uri,
                        name = name,
                        dateModified = lastModified,
                        dateTaken = lastModified
                    )
                )
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
            MediaStore.Images.Media.DATE_TAKEN,
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
            val takenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val dateModified = cursor.getLong(modifiedColumn)
                // DATE_TAKEN is in milliseconds; DATE_MODIFIED is in seconds
                val dateTaken = cursor.getLong(takenColumn)
                val uri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                photos.add(
                    PhotoItem(
                        id = id.toString(),
                        uri = uri,
                        name = name,
                        dateModified = dateModified * 1000,
                        dateTaken = if (dateTaken > 0) dateTaken else dateModified * 1000
                    )
                )
            }
        }
        photos
    }

    fun getThumbnailUri(photo: PhotoItem): Uri = photo.uri
}
