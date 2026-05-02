package com.rrajath.occullt.feature.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.core.datastore.LocalPhotoRepository
import com.rrajath.occullt.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LibraryState(
    val photos: List<com.rrajath.occullt.core.model.PhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val folderUri: String? = null,
    val markedIds: Set<String> = emptySet(),
    val pinnedId: String? = null,
)

class LibraryViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val localPhotoRepository = LocalPhotoRepository(context)

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            val folderUriStr = settingsRepository.libraryFolderUri.first()
            if (folderUriStr == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }

            _state.value = _state.value.copy(isLoading = true)

            val folderUri = Uri.parse(folderUriStr)
            val photos = localPhotoRepository.getPhotosFromFolder(folderUri)
            val markedIds = settingsRepository.markedIds.first()
            val pinnedId = settingsRepository.pinnedId.first()

            _state.value = _state.value.copy(
                photos = photos,
                isLoading = false,
                folderUri = folderUriStr,
                markedIds = markedIds,
                pinnedId = pinnedId
            )
        }
    }

    fun setFolderUri(uri: Uri) {
        viewModelScope.launch {
            localPhotoRepository.takePersistablePermission(uri)
            settingsRepository.setLibraryFolderUri(uri.toString())
            loadPhotos()
        }
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
