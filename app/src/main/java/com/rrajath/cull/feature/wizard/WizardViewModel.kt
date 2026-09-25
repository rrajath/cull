package com.rrajath.cull.feature.wizard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rrajath.cull.core.database.WizardSegment
import com.rrajath.cull.core.database.WizardSegmentDb
import com.rrajath.cull.core.database.WizardSegmentState
import com.rrajath.cull.core.database.WizardSegmentStore
import com.rrajath.cull.core.datastore.SettingsRepository
import com.rrajath.cull.ui.component.SourceMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

data class WizardMonthCell(
    val monthKey: String,
    val label: String,
    val photoCount: Int,
    val state: WizardSegmentState,
)

data class WizardYearGroup(
    val year: Int,
    val cells: List<WizardMonthCell>,
)

data class WizardInProgressCard(
    val monthKey: String,
    val title: String,
    val photoCount: Int,
    val lastAccessedAt: Long?,
)

data class WizardState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val inProgress: List<WizardInProgressCard> = emptyList(),
    val years: List<WizardYearGroup> = emptyList(),
    val visibleMonthCount: Int = 0,
    val sourceMode: SourceMode = SourceMode.Hybrid,
    val scanError: String? = null,
) {
    val hasAnyProgress: Boolean
        get() = inProgress.isNotEmpty() ||
            years.any { group -> group.cells.any { it.state != WizardSegmentState.NOT_STARTED } }
}

class WizardViewModel(
    private val settingsRepository: SettingsRepository,
    private val store: WizardSegmentStore,
    private val photoLoader: WizardPhotoLoader,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    private var isScanInFlight = false

    fun load() {
        viewModelScope.launch {
            val sourceMode = settingsRepository.sourceMode.first()

            // Cached-first: render whatever the last scan left in the DB
            val cached = withContext(ioDispatcher) { store.getAll() }
            _state.value = buildState(cached, sourceMode, isScanning = true)

            if (isScanInFlight) return@launch
            isScanInFlight = true
            try {
                val folderUri = settingsRepository.libraryFolderUri.first()
                val result = photoLoader.load(sourceMode, folderUri)
                result.fold(
                    onSuccess = { photos ->
                        WizardScanCache.setScan(photos)
                        val refreshed = withContext(ioDispatcher) {
                            store.upsertPhotoCounts(WizardSegmentation.countByMonth(photos, zone))
                            store.getAll()
                        }
                        _state.value = buildState(refreshed, sourceMode, isScanning = false)
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isScanning = false,
                            scanError = e.message ?: "Failed to scan library",
                        )
                    },
                )
            } finally {
                isScanInFlight = false
            }
        }
    }

    private fun buildState(
        segments: List<WizardSegment>,
        sourceMode: SourceMode,
        isScanning: Boolean,
    ): WizardState {
        val currentMonthKey = WizardSegmentation.monthKey(nowMs(), zone)
        val byKey = segments.associateBy { it.monthKey }.toMutableMap()
        // The current calendar month is always shown, even with no photos yet
        byKey.getOrPut(currentMonthKey) {
            WizardSegment(
                monthKey = currentMonthKey,
                state = WizardSegmentState.NOT_STARTED,
                photoCount = 0,
                deletedCount = 0,
                startedAt = null,
                completedAt = null,
                lastAccessedAt = null,
            )
        }

        val visible = byKey.values.filter { it.photoCount > 0 || it.monthKey == currentMonthKey }

        val years = visible
            .groupBy { WizardSegmentation.year(it.monthKey) }
            .toSortedMap(reverseOrder())
            .map { (year, monthSegments) ->
                WizardYearGroup(
                    year = year,
                    cells = monthSegments
                        .sortedByDescending { it.monthKey }
                        .map { segment ->
                            WizardMonthCell(
                                monthKey = segment.monthKey,
                                label = WizardSegmentation.monthAbbreviation(segment.monthKey),
                                photoCount = segment.photoCount,
                                state = segment.state,
                            )
                        },
                )
            }

        val inProgress = visible
            .filter { it.state == WizardSegmentState.IN_PROGRESS }
            .sortedByDescending { it.lastAccessedAt ?: 0L }
            .map { segment ->
                WizardInProgressCard(
                    monthKey = segment.monthKey,
                    title = WizardSegmentation.monthTitle(segment.monthKey),
                    photoCount = segment.photoCount,
                    lastAccessedAt = segment.lastAccessedAt,
                )
            }

        return WizardState(
            isLoading = false,
            isScanning = isScanning,
            inProgress = inProgress,
            years = years,
            visibleMonthCount = visible.size,
            sourceMode = sourceMode,
            scanError = null,
        )
    }
}

class WizardViewModelFactory(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WizardViewModel::class.java)) {
            return WizardViewModel(
                settingsRepository = settingsRepository,
                store = WizardSegmentDb.getInstance(context),
                photoLoader = defaultWizardPhotoLoader(context, settingsRepository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
