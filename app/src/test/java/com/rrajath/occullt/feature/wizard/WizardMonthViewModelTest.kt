package com.rrajath.occullt.feature.wizard

import android.net.Uri
import com.rrajath.occullt.core.database.WizardSegment
import com.rrajath.occullt.core.database.WizardSegmentState
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import com.rrajath.occullt.feature.stacks.PhotoStackCache
import com.rrajath.occullt.ui.component.SourceMode
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class WizardMonthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUri = mockk<Uri>(relaxed = true)
    private val zone = ZoneId.of("UTC")
    private val fixedNow =
        ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var store: FakeWizardSegmentStore

    private val failingLoader = WizardPhotoLoader { _, _ ->
        Result.failure(Exception("loader should not be called"))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.sourceMode } returns flowOf(SourceMode.Hybrid)
        every { settingsRepository.libraryFolderUri } returns flowOf(null)
        every { settingsRepository.groupingWindowMinutes } returns flowOf(2)
        every { settingsRepository.markedIds } returns flowOf(emptySet())
        store = FakeWizardSegmentStore()
        WizardScanCache.clear()
        PhotoStackCache.stacks = emptyMap()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        WizardScanCache.clear()
        PhotoStackCache.stacks = emptyMap()
    }

    private fun photoAt(id: String, month: Int, day: Int, hour: Int, minute: Int) = UnifiedPhotoItem(
        id = id,
        uri = mockUri,
        name = "$id.jpg",
        dateTaken = ZonedDateTime.of(2026, month, day, hour, minute, 0, 0, zone)
            .toInstant().toEpochMilli(),
    )

    private fun viewModel(
        monthKey: String = "2026-01",
        loader: WizardPhotoLoader = failingLoader,
    ) = WizardMonthViewModel(
        monthKey = monthKey,
        settingsRepository = settingsRepository,
        store = store,
        photoLoader = loader,
        zone = zone,
        nowMs = { fixedNow },
        ioDispatcher = testDispatcher,
        defaultDispatcher = testDispatcher,
    )

    @Test
    fun `onEnter transitions not started segment to in progress`() = runTest {
        val vm = viewModel()
        vm.onEnter()
        advanceUntilIdle()

        assertEquals(WizardSegmentState.IN_PROGRESS, store.get("2026-01")?.state)
        assertEquals(fixedNow, store.get("2026-01")?.startedAt)
        assertEquals(WizardSegmentState.IN_PROGRESS, vm.state.value.segmentState)
    }

    @Test
    fun `onEnter leaves complete segment complete`() = runTest {
        store.seed(
            WizardSegment(
                monthKey = "2026-01",
                state = WizardSegmentState.COMPLETE,
                photoCount = 47,
                deletedCount = 3,
                startedAt = 1L,
                completedAt = 2L,
                lastAccessedAt = 2L,
            )
        )
        val vm = viewModel()
        vm.onEnter()
        advanceUntilIdle()

        val segment = store.get("2026-01")
        assertEquals(WizardSegmentState.COMPLETE, segment?.state)
        assertEquals(fixedNow, segment?.lastAccessedAt)
        assertEquals(WizardSegmentState.COMPLETE, vm.state.value.segmentState)
        assertEquals(3, vm.state.value.deletedCount)
    }

    @Test
    fun `loadStacks filters scan to the month and groups by dateTaken`() = runTest {
        WizardScanCache.setScan(
            listOf(
                // burst in January: one stack of 2
                photoAt("1", 1, 8, 10, 15),
                photoAt("2", 1, 8, 10, 16),
                // lone January photo: dropped from stacks, still counted
                photoAt("3", 1, 22, 14, 0),
                // February photo: outside the month
                photoAt("4", 2, 1, 0, 0),
            )
        )
        val vm = viewModel()
        vm.loadStacks()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(3, state.photoCount)
        assertEquals(1, state.stacks.size)
        assertEquals(listOf("1", "2"), state.stacks[0].photos.map { it.id })
        // PhotoStackCache populated for StackGrid navigation
        assertEquals(listOf("1", "2"), PhotoStackCache.stacks[0]?.map { it.id })
    }

    @Test
    fun `markedCount counts only marked photos within the month`() = runTest {
        every { settingsRepository.markedIds } returns flowOf(setOf("1", "4", "unknown"))
        WizardScanCache.setScan(
            listOf(
                photoAt("1", 1, 8, 10, 15),
                photoAt("2", 1, 8, 10, 16),
                photoAt("4", 2, 1, 0, 0),
            )
        )
        val vm = viewModel()
        vm.loadStacks()
        advanceUntilIdle()

        assertEquals(1, vm.state.value.markedCount)
    }

    @Test
    fun `markComplete persists complete state`() = runTest {
        val vm = viewModel()
        vm.onEnter()
        advanceUntilIdle()

        vm.markComplete()
        advanceUntilIdle()

        assertEquals(WizardSegmentState.COMPLETE, store.get("2026-01")?.state)
        assertEquals(fixedNow, store.get("2026-01")?.completedAt)
        assertEquals(WizardSegmentState.COMPLETE, vm.state.value.segmentState)
    }

    @Test
    fun `reload trigger forces refresh and is deduplicated`() = runTest {
        WizardScanCache.setScan(
            listOf(photoAt("1", 1, 8, 10, 15), photoAt("2", 1, 8, 10, 16))
        )
        var loaderCalls = 0
        val vm = viewModel(loader = { _, _ ->
            loaderCalls++
            Result.success(listOf(photoAt("1", 1, 8, 10, 15)))
        })

        vm.onReloadTrigger(1)
        advanceUntilIdle()
        assertEquals(1, loaderCalls)
        assertEquals(1, vm.state.value.photoCount)

        // same trigger value again: no extra reload
        vm.onReloadTrigger(1)
        advanceUntilIdle()
        assertEquals(1, loaderCalls)
    }
}
