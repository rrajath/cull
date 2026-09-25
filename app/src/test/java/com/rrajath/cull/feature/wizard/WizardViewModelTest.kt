package com.rrajath.cull.feature.wizard

import android.net.Uri
import com.rrajath.cull.core.database.WizardSegment
import com.rrajath.cull.core.database.WizardSegmentState
import com.rrajath.cull.core.datastore.SettingsRepository
import com.rrajath.cull.core.model.UnifiedPhotoItem
import com.rrajath.cull.ui.component.SourceMode
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class WizardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUri = mockk<Uri>(relaxed = true)
    private val zone = ZoneId.of("UTC")

    // "now" is June 15, 2026
    private val fixedNow =
        ZonedDateTime.of(2026, 6, 15, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var store: FakeWizardSegmentStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.sourceMode } returns flowOf(SourceMode.Hybrid)
        every { settingsRepository.libraryFolderUri } returns flowOf(null)
        store = FakeWizardSegmentStore()
        WizardScanCache.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        WizardScanCache.clear()
    }

    private fun segment(
        monthKey: String,
        state: WizardSegmentState = WizardSegmentState.NOT_STARTED,
        photoCount: Int = 10,
        lastAccessedAt: Long? = null,
    ) = WizardSegment(
        monthKey = monthKey,
        state = state,
        photoCount = photoCount,
        deletedCount = 0,
        startedAt = null,
        completedAt = null,
        lastAccessedAt = lastAccessedAt,
    )

    private fun photoTakenAt(id: String, year: Int, month: Int, day: Int) = UnifiedPhotoItem(
        id = id,
        uri = mockUri,
        name = "$id.jpg",
        dateTaken = ZonedDateTime.of(year, month, day, 10, 0, 0, 0, zone)
            .toInstant().toEpochMilli(),
    )

    private fun viewModel(loader: WizardPhotoLoader) = WizardViewModel(
        settingsRepository = settingsRepository,
        store = store,
        photoLoader = loader,
        zone = zone,
        nowMs = { fixedNow },
        ioDispatcher = testDispatcher,
    )

    @Test
    fun `load renders cached segments even when scan fails`() = runTest {
        store.seed(segment("2026-01", WizardSegmentState.COMPLETE, photoCount = 47))
        val vm = viewModel { _, _ -> Result.failure(Exception("offline")) }

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isScanning)
        assertEquals("offline", state.scanError)
        val cells = state.years.flatMap { it.cells }
        assertNotNull(cells.find { it.monthKey == "2026-01" && it.photoCount == 47 })
    }

    @Test
    fun `scan counts merge with existing segment states`() = runTest {
        store.seed(segment("2026-01", WizardSegmentState.IN_PROGRESS, photoCount = 50, lastAccessedAt = 1L))
        val photos = listOf(
            photoTakenAt("1", 2026, 1, 5),
            photoTakenAt("2", 2026, 1, 20),
            photoTakenAt("3", 2025, 12, 31),
        )
        val vm = viewModel { _, _ -> Result.success(photos) }

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.scanError)
        val cells = state.years.flatMap { it.cells }
        val january = cells.find { it.monthKey == "2026-01" }
        assertEquals(2, january?.photoCount)
        assertEquals(WizardSegmentState.IN_PROGRESS, january?.state)
        assertEquals(1, cells.count { it.monthKey == "2025-12" })
        // scan result cached for the month view
        assertEquals(3, WizardScanCache.getScan()?.size)
    }

    @Test
    fun `zero-count months are hidden except the current month`() = runTest {
        store.seed(segment("2025-03", WizardSegmentState.COMPLETE, photoCount = 0))
        val vm = viewModel { _, _ -> Result.success(emptyList()) }

        vm.load()
        advanceUntilIdle()

        val cells = vm.state.value.years.flatMap { it.cells }
        assertNull(cells.find { it.monthKey == "2025-03" })
        // current month (June 2026) is always shown even with 0 photos
        val current = cells.find { it.monthKey == "2026-06" }
        assertNotNull(current)
        assertEquals(0, current?.photoCount)
    }

    @Test
    fun `in progress cards are sorted by most recently accessed`() = runTest {
        store.seed(segment("2025-03", WizardSegmentState.IN_PROGRESS, lastAccessedAt = 100L))
        store.seed(segment("2026-01", WizardSegmentState.IN_PROGRESS, lastAccessedAt = 500L))
        val vm = viewModel { _, _ -> Result.failure(Exception("skip scan")) }

        vm.load()
        advanceUntilIdle()

        val inProgress = vm.state.value.inProgress
        assertEquals(listOf("2026-01", "2025-03"), inProgress.map { it.monthKey })
        assertEquals("January 2026", inProgress[0].title)
    }

    @Test
    fun `guidance state when nothing started`() = runTest {
        val photos = listOf(photoTakenAt("1", 2026, 1, 5), photoTakenAt("2", 2026, 1, 6))
        val vm = viewModel { _, _ -> Result.success(photos) }

        vm.load()
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state.inProgress.isEmpty())
        assertFalse(state.hasAnyProgress)
    }

    @Test
    fun `years are sorted newest first with months newest first`() = runTest {
        val photos = listOf(
            photoTakenAt("1", 2025, 7, 1),
            photoTakenAt("2", 2025, 12, 1),
            photoTakenAt("3", 2026, 2, 1),
        )
        val vm = viewModel { _, _ -> Result.success(photos) }

        vm.load()
        advanceUntilIdle()

        val years = vm.state.value.years
        assertEquals(listOf(2026, 2025), years.map { it.year })
        // 2026: current month (Jun) synthesized + Feb
        assertEquals(listOf("2026-06", "2026-02"), years[0].cells.map { it.monthKey })
        assertEquals(listOf("2025-12", "2025-07"), years[1].cells.map { it.monthKey })
    }
}
