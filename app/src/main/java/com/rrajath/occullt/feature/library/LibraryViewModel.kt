package com.rrajath.occullt.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.OcculltApplication
import com.rrajath.occullt.core.database.ImmichAssetMappingDb
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class LibraryState(
    val photos: List<UnifiedPhotoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val folderUri: String? = null,
    val markedIds: Set<String> = emptySet(),
    val pinnedId: String? = null,
    val sourceMode: SourceMode = SourceMode.Local,
    val error: String? = null,
    val hasMore: Boolean = false,
    val createdAfter: String? = null,
    val createdBefore: String? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val filterPage: Int = 1,
) {
    val isFilterActive: Boolean get() = filterStartDate != null || filterEndDate != null
}

class LibraryViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    companion object {
        private const val EPOCH_START = "1970-01-01T00:00:00.000Z"
        private const val PAGE_SIZE = 1000
    }

    private val utcDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private var hasLoadedPhotos = false

    fun loadPhotos(forceReload: Boolean = false) {
        if (!forceReload && hasLoadedPhotos && _state.value.photos.isNotEmpty()) {
            _state.value = _state.value.copy(isLoading = false)
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val sourceMode = settingsRepository.sourceMode.first()
            val folderUri = settingsRepository.libraryFolderUri.first()
            val immichUrl = settingsRepository.immichUrl.first()
            val immichApiKey = settingsRepository.immichApiKey.first()
            val markedIds = settingsRepository.markedIds.first()
            val pinnedId = settingsRepository.pinnedId.first()

            val immichRepo = if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                OcculltApplication.setImmichApiKey(immichApiKey)
                val mappingDb = ImmichAssetMappingDb.getInstance(context)
                ImmichRepository(ImmichApi(immichUrl, immichApiKey), mappingDb)
            } else {
                OcculltApplication.setImmichApiKey(null)
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

            if (sourceMode == SourceMode.Immich || sourceMode == SourceMode.Hybrid) {
                val filterStartDateMs = _state.value.filterStartDate
                val filterEndDateMs = _state.value.filterEndDate

                // Page from the beginning of time via /api/search/metadata;
                // "load more" advances the page rather than rolling a date window
                val createdAfterStr = if (filterStartDateMs != null) {
                    utcDateFormat.format(Date(filterStartDateMs))
                } else {
                    EPOCH_START
                }
                val createdBeforeStr = filterEndDateMs?.let { utcDateFormat.format(Date(it)) }

                val result = unifiedRepo.loadPhotosPaginated(
                    sourceMode = sourceMode,
                    folderUri = folderUri,
                    createdAfter = createdAfterStr,
                    createdBefore = createdBeforeStr,
                    page = 1,
                    size = PAGE_SIZE,
                )

                if (result.isSuccess) {
                    val paginated = result.getOrThrow()
                    var allPhotos = paginated.photos
                    if (sourceMode == SourceMode.Hybrid) {
                        allPhotos = allPhotos.filter { photo ->
                            (filterStartDateMs == null || photo.dateModified >= filterStartDateMs) &&
                            (filterEndDateMs == null || photo.dateModified <= filterEndDateMs)
                        }
                    }
                    hasLoadedPhotos = true
                    _state.value = _state.value.copy(
                        photos = allPhotos,
                        isLoading = false,
                        folderUri = folderUri,
                        markedIds = markedIds,
                        pinnedId = pinnedId,
                        sourceMode = sourceMode,
                        hasMore = paginated.hasNextPage,
                        createdAfter = createdAfterStr,
                        createdBefore = createdBeforeStr,
                        filterPage = 1,
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load photos"
                    )
                }
            } else {
                val result = unifiedRepo.loadPhotos(sourceMode, folderUri)

                if (result.isSuccess) {
                    val allPhotos = result.getOrNull().orEmpty()
                    val filterStartDateMs = _state.value.filterStartDate
                    val filterEndDateMs = _state.value.filterEndDate
                    val filteredPhotos = if (_state.value.isFilterActive) {
                        allPhotos.filter { photo ->
                            (filterStartDateMs == null || photo.dateModified >= filterStartDateMs) &&
                            (filterEndDateMs == null || photo.dateModified <= filterEndDateMs)
                        }
                    } else {
                        allPhotos
                    }
                    hasLoadedPhotos = true
                    _state.value = _state.value.copy(
                        photos = filteredPhotos,
                        isLoading = false,
                        folderUri = folderUri,
                        markedIds = markedIds,
                        pinnedId = pinnedId,
                        sourceMode = sourceMode,
                        hasMore = false,
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Failed to load photos"
                    )
                }
            }
        }
    }

    fun loadMorePhotos() {
        if (_state.value.isLoadingMore || !_state.value.hasMore) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            val sourceMode = settingsRepository.sourceMode.first()
            val folderUri = settingsRepository.libraryFolderUri.first()
            val immichUrl = settingsRepository.immichUrl.first()
            val immichApiKey = settingsRepository.immichApiKey.first()

            val immichRepo = if (sourceMode != SourceMode.Local && !immichUrl.isNullOrBlank() && !immichApiKey.isNullOrBlank()) {
                OcculltApplication.setImmichApiKey(immichApiKey)
                val mappingDb = ImmichAssetMappingDb.getInstance(context)
                ImmichRepository(ImmichApi(immichUrl, immichApiKey), mappingDb)
            } else {
                OcculltApplication.setImmichApiKey(null)
                null
            }

            val unifiedRepo = UnifiedPhotoRepository(context, immichRepo)
            val currentPhotos = _state.value.photos.toMutableList()

            val nextPage = _state.value.filterPage + 1
            val filterStartDateMs = _state.value.filterStartDate
            val filterEndDateMs = _state.value.filterEndDate

            val result = unifiedRepo.loadPhotosPaginated(
                sourceMode = sourceMode,
                folderUri = folderUri,
                createdAfter = _state.value.createdAfter ?: EPOCH_START,
                createdBefore = _state.value.createdBefore,
                page = nextPage,
                size = PAGE_SIZE,
            )

            if (result.isSuccess) {
                val paginated = result.getOrThrow()
                var newPhotos = paginated.photos
                if (sourceMode == SourceMode.Hybrid && _state.value.isFilterActive) {
                    newPhotos = newPhotos.filter { photo ->
                        (filterStartDateMs == null || photo.dateModified >= filterStartDateMs) &&
                        (filterEndDateMs == null || photo.dateModified <= filterEndDateMs)
                    }
                }
                currentPhotos.addAll(newPhotos)
                currentPhotos.sortByDescending { it.dateModified }

                _state.value = _state.value.copy(
                    photos = currentPhotos,
                    isLoadingMore = false,
                    hasMore = paginated.hasNextPage,
                    filterPage = nextPage,
                )
            } else {
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = result.exceptionOrNull()?.message ?: "Failed to load more photos"
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

    fun setDateFilter(startDate: Long?, endDate: Long?) {
        _state.value = _state.value.copy(
            filterStartDate = startDate,
            filterEndDate = endDate,
        )
        loadPhotos(forceReload = true)
    }

    fun clearDateFilter() {
        _state.value = _state.value.copy(
            filterStartDate = null,
            filterEndDate = null,
        )
        loadPhotos(forceReload = true)
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
