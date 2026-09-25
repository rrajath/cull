package com.rrajath.cull.feature.stacks

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.cull.core.datastore.PhotoCache
import com.rrajath.cull.core.datastore.SettingsRepository
import com.rrajath.cull.core.grouping.PhotoStack
import com.rrajath.cull.core.grouping.groupPhotos
import com.rrajath.cull.core.model.UnifiedPhotoItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StacksState(
    val groups: List<PhotoStack> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
)

object PhotoStackCache {
    var stacks: Map<Int, List<UnifiedPhotoItem>> = emptyMap()

    fun removePhotos(ids: Set<String>) {
        if (ids.isEmpty()) return
        stacks = stacks.mapValues { (_, photos) -> photos.filterNot { ids.contains(it.id) } }
    }
}

class StacksViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(StacksState())
    val state: StateFlow<StacksState> = _state.asStateFlow()

    private var cachedPhotos: List<UnifiedPhotoItem>? = null
    private var lastHandledReloadTrigger = 0
    private val _internalReload = MutableStateFlow(0)

    fun loadGroups() {
        viewModelScope.launch {
            combine(
                settingsRepository.groupingWindowMinutes,
                _internalReload
            ) { windowMinutes, _ -> windowMinutes }.collectLatest { windowMinutes ->
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

    /** Called when the global reload trigger fires (e.g. after a delete). */
    fun onReloadTrigger(triggerValue: Int) {
        if (triggerValue <= lastHandledReloadTrigger) return
        lastHandledReloadTrigger = triggerValue
        // photos deleted since the last load were already scrubbed from
        // PhotoCache by the deleting screen — re-pull and regroup
        cachedPhotos = null
        _internalReload.value++
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
