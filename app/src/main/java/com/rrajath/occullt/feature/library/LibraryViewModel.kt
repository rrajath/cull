package com.rrajath.occullt.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.datastore.UnifiedPhotoRepository
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.core.network.ImmichApi
import com.rrajath.occullt.core.network.ImmichRepository
import com.rrajath.occullt.ui.component.SourceMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LibraryState(
    val photos: List<UnifiedPhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val folderUri: String? = null,
    val markedIds: Set<String> = emptySet(),
    val pinnedId: String? = null,
    val sourceMode: SourceMode = SourceMode.Local,
    val error: String? = null,
)

class LibraryViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    fun loadPhotos() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val sourceMode = settingsRepository.sourceMode.first()
            val folderUri = settingsRepository.libraryFolderUri.first()
            val immichUrl = settingsRepository.immichUrl.first()
            val immichApiKey = settingsRepository.immichApiKey.first()
            val markedIds = settingsRepository.markedIds.first()
            val pinnedId = settingsRepository.pinnedId.first()

            val immichRepo = if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                ImmichRepository(ImmichApi(immichUrl, immichApiKey))
            } else {
                null
            }

            val unifiedRepo = UnifiedPhotoRepository(context, immichRepo)

            if (sourceMode == SourceMode.Local && folderUri.isNullOrBlank()) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Permission denied"
                    )
                    return@launch
                }
            }

            val result = unifiedRepo.loadPhotos(sourceMode, folderUri)

            if (result.isSuccess) {
                val photos = result.getOrNull().orEmpty()
                _state.value = _state.value.copy(
                    photos = photos,
                    isLoading = false,
                    folderUri = folderUri,
                    markedIds = markedIds,
                    pinnedId = pinnedId,
                    sourceMode = sourceMode,
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load photos"
                )
            }
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
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
