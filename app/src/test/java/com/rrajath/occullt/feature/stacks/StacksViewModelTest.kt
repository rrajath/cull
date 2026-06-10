package com.rrajath.occullt.feature.stacks

import android.content.Context
import android.net.Uri
import com.rrajath.occullt.core.datastore.PhotoCache
import com.rrajath.occullt.core.datastore.SettingsRepository
import com.rrajath.occullt.core.model.PhotoSource
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StacksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockUri = mockk<Uri>(relaxed = true)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        every { settingsRepository.groupingWindowMinutes } returns flowOf(2)
        PhotoCache.clear()
        PhotoStackCache.stacks = emptyMap()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        PhotoCache.clear()
        PhotoStackCache.stacks = emptyMap()
    }

    @Test
    fun `loadGroups with empty cache shows empty state`() = runTest {
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `loadGroups with one photo shows empty state`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                )
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertTrue(state.groups.isEmpty())
    }

    @Test
    fun `loadGroups with two close photos forms one stack`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 2000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertEquals(1, state.groups.size)

        val stack = state.groups[0]
        assertEquals(2, stack.photos.size)
        assertTrue(stack.startTime <= stack.endTime)
    }

    @Test
    fun `loadGroups with photos far apart forms no stacks`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 200_000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `loadGroups with multiple clusters forms multiple stacks`() = runTest {
        // Window = 2 minutes = 120,000 ms
        // Cluster 1: id="1" at 1000, id="2" at 2000 (within 2 min)
        // Cluster 2: id="3" at 130_000, id="4" at 131_000 (within 2 min)
        // Cluster 3 (solo): id="5" at 300_000 (should be discarded)
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "5", uri = mockUri, name = "photo5.jpg",
                    dateModified = 300_000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "4", uri = mockUri, name = "photo4.jpg",
                    dateModified = 131_000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "3", uri = mockUri, name = "photo3.jpg",
                    dateModified = 130_000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 2000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        // Two stacks: (1000, 2000) and (130000, 131000); 300000 is solo
        assertEquals(2, state.groups.size)
    }

    @Test
    fun `loadGroups updates PhotoStackCache`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 2000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        assertFalse(PhotoStackCache.stacks.isEmpty())
        assertEquals(1, PhotoStackCache.stacks.size)
    }

    @Test
    fun `loadGroups with three photos within window forms one stack`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "3", uri = mockUri, name = "photo3.jpg",
                    dateModified = 3000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 2000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.groups.size)
        assertEquals(3, state.groups[0].photos.size)
    }

    @Test
    fun `loadGroups startTime is earliest photo in stack`() = runTest {
        PhotoCache.setPhotos(
            listOf(
                UnifiedPhotoItem(
                    id = "2", uri = mockUri, name = "photo2.jpg",
                    dateModified = 2000L, source = PhotoSource.Local
                ),
                UnifiedPhotoItem(
                    id = "1", uri = mockUri, name = "photo1.jpg",
                    dateModified = 1000L, source = PhotoSource.Local
                ),
            )
        )
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val stack = viewModel.state.value.groups[0]
        assertEquals(1000L, stack.startTime)
        assertEquals(2000L, stack.endTime)
    }

    @Test
    fun `loadGroups with all photos within window uses all photos`() = runTest {
        val photos = (1..5).map { i ->
            UnifiedPhotoItem(
                id = "$i", uri = mockUri, name = "photo$i.jpg",
                dateModified = (i * 10_000L), source = PhotoSource.Local
            )
        }
        PhotoCache.setPhotos(photos)
        val viewModel = StacksViewModel(context, settingsRepository)
        viewModel.loadGroups()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.groups.size)
        assertEquals(5, state.groups[0].photos.size)
    }
}
