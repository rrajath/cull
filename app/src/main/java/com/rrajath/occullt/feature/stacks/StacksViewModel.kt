package com.rrajath.occullt.feature.stacks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.grouping.PhotoStack
import com.rrajath.occullt.core.grouping.groupPhotos
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StacksState(
    val groups: List<PhotoStack> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

object PhotoStackCache {
    var stacks: Map<Int, List<UnifiedPhotoItem>> = emptyMap()
}

class StacksViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(StacksState())
    val state: StateFlow<StacksState> = _state.asStateFlow()

    private var cachedPhotos: List<UnifiedPhotoItem>? = null

    fun loadGroups() {
        viewModelScope.launch {
            settingsRepository.groupingWindowMinutes.collectLatest { windowMinutes ->
                _state.value = StacksState(isLoading = true)

                val windowMs = windowMinutes * 60 * 1000L

                if (cachedPhotos == null) {
                    cachedPhotos = PhotoCache.getPhotos()
                }
                val photos = cachedPhotos ?: emptyList()
                if (photos.isEmpty()) {
                    _state.value = StacksState(isLoading = false, isEmpty = true)
                    return@collectLatest
                }

                // sorting + grouping the full library can take tens of ms —
                // keep it off the main thread so screen entry doesn't drop frames
                val groups = withContext(defaultDispatcher) {
                    groupPhotos(photos.sortedBy { it.dateModified }, windowMs)
                }

                PhotoStackCache.stacks = groups.mapIndexed { index, stack ->
                    index to stack.photos
                }.toMap()

                _state.value = StacksState(
                    groups = groups,
                    isLoading = false,
                    isEmpty = groups.isEmpty(),
                )
            }
        }
    }

}

class StacksViewModelFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StacksViewModel::class.java)) {
            return StacksViewModel(context, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
