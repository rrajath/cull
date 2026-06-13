package com.rrajath.occullt.feature.wizard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.occullt.core.database.WizardSegmentDb
import com.rrajath.occullt.core.database.WizardSegmentState
import com.rrajath.occullt.core.database.WizardSegmentStore
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.grouping.PhotoStack
import com.rrajath.occullt.core.grouping.groupPhotos
import com.rrajath.occullt.feature.stacks.PhotoStackCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

data class WizardMonthState(
    val title: String = "",
    val isLoading: Boolean = true,
    val photoCount: Int = 0,
    val stacks: List<PhotoStack> = emptyList(),
    val segmentState: WizardSegmentState = WizardSegmentState.NOT_STARTED,
    val markedCount: Int = 0,
    val deletedCount: Int = 0,
    val error: String? = null,
)

class WizardMonthViewModel(
    private val monthKey: String,
    private val settingsRepository: SettingsRepository,
    private val store: WizardSegmentStore,
    private val photoLoader: WizardPhotoLoader,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WizardMonthState(title = WizardSegmentation.monthTitle(monthKey))
    )
    val state: StateFlow<WizardMonthState> = _state.asStateFlow()

    private var monthPhotoIds: Set<String> = emptySet()
    private var lastHandledReloadTrigger = 0

    init {
        viewModelScope.launch {
            settingsRepository.markedIds.collectLatest { ids ->
                _state.value = _state.value.copy(markedCount = ids.intersect(monthPhotoIds).size)
            }
        }
    }

    /** First-entry state transition: NOT_STARTED → IN_PROGRESS; otherwise just touch. */
    fun onEnter() {
        viewModelScope.launch(ioDispatcher) {
            val existing = store.get(monthKey)
            if (existing == null || existing.state == WizardSegmentState.NOT_STARTED) {
                store.markInProgressIfNotStarted(monthKey, nowMs())
            } else {
                store.touch(monthKey, nowMs())
            }
            val segment = store.get(monthKey)
            if (segment != null) {
                _state.value = _state.value.copy(
                    segmentState = segment.state,
                    deletedCount = segment.deletedCount,
                )
            }
        }
    }

    fun loadStacks(forceReload: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val scan = if (forceReload) null else WizardScanCache.getScan()
            val photos = scan ?: run {
                val sourceMode = settingsRepository.sourceMode.first()
                val folderUri = settingsRepository.libraryFolderUri.first()
                val result = photoLoader.load(sourceMode, folderUri)
                result.fold(
                    onSuccess = { loaded ->
                        WizardScanCache.setScan(loaded)
                        loaded
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load photos",
                        )
                        return@launch
                    },
                )
            }

            val windowMinutes = settingsRepository.groupingWindowMinutes.first()
            val windowMs = windowMinutes * 60 * 1000L

            // filter + sort + group off the main thread so the screen's entry
            // animation doesn't drop frames on large libraries
            val (monthPhotos, stacks) = withContext(defaultDispatcher) {
                val (monthStart, monthEnd) = WizardSegmentation.monthBounds(monthKey, zone)
                val filtered = photos.filter { it.dateTaken in monthStart until monthEnd }
                val grouped = groupPhotos(filtered.sortedBy { it.dateTaken }, windowMs) { it.dateTaken }
                filtered to grouped
            }
            monthPhotoIds = monthPhotos.map { it.id }.toSet()

            PhotoStackCache.stacks = stacks.mapIndexed { index, stack ->
                index to stack.photos
            }.toMap()

            val markedIds = settingsRepository.markedIds.first()

            _state.value = _state.value.copy(
                isLoading = false,
                photoCount = monthPhotos.size,
                stacks = stacks,
                markedCount = markedIds.intersect(monthPhotoIds).size,
            )
        }
    }

    /** Called when the global reload trigger fires (e.g. after a delete). */
    fun onReloadTrigger(triggerValue: Int) {
        if (triggerValue <= lastHandledReloadTrigger) return
        lastHandledReloadTrigger = triggerValue
        loadStacks(forceReload = true)
        // refresh deletedCount, which the delete flow may have incremented
        viewModelScope.launch(ioDispatcher) {
            store.get(monthKey)?.let { segment ->
                _state.value = _state.value.copy(deletedCount = segment.deletedCount)
            }
        }
    }

    suspend fun markComplete() {
        withContext(ioDispatcher) {
            store.markComplete(monthKey, nowMs())
        }
        _state.value = _state.value.copy(segmentState = WizardSegmentState.COMPLETE)
    }
}

class WizardMonthViewModelFactory(
    private val context: Context,
    private val monthKey: String,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WizardMonthViewModel::class.java)) {
            return WizardMonthViewModel(
                monthKey = monthKey,
                settingsRepository = settingsRepository,
                store = WizardSegmentDb.getInstance(context),
                photoLoader = defaultWizardPhotoLoader(context, settingsRepository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
