package com.rrajath.occullt.feature.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryState(
    val photos: List<PhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val folderUri: String? = null,
    val markedIds: Set<String> = emptySet(),
    val pinnedId: String? = null,
)

class LibraryViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    fun loadCameraPhotos(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val photos = loadPhotosFromMediaStore(context)
            val markedIds = settingsRepository.markedIds.first()
            val pinnedId = settingsRepository.pinnedId.first()

            _state.value = _state.value.copy(
                photos = photos,
                isLoading = false,
                folderUri = "mediastore",
                markedIds = markedIds,
                pinnedId = pinnedId
            )
        }
    }

    private suspend fun loadPhotosFromMediaStore(context: Context): List<PhotoItem> = withContext(Dispatchers.IO) {
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

    fun toggleMarked(photoId: String) {
        viewModelScope.launch {
            val current = _state.value.markedIds.toMutableSet()
            if (current.contains(photoId)) {
                current.remove(photoId)
            } else {
                current.add(photoId)
            }
            _state.value = _state.value.copy(markedIds = current)
            settingsRepository.setMarkedIds(current)
        }
    }

    fun setPinnedId(id: String?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(pinnedId = id)
            settingsRepository.setPinnedId(id)
        }
    }
}

class LibraryViewModelFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
            return LibraryViewModel(context, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
