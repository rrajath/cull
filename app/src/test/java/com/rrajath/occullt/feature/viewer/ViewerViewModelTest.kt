package com.rrajath.occullt.feature.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ViewerViewModelTest {

    private lateinit var viewModel: ViewerViewModel

    @Before
    fun setUp() {
        viewModel = ViewerViewModel()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value
        assertEquals(0, state.currentIndex)
        assertFalse(state.isHudOpen)
        assertNull(state.pinnedId)
        assertFalse(state.isLongPressing)
        assertTrue(state.isLoading)
        assertTrue(state.markedIds.isEmpty())
        assertFalse(state.isMarked)
        assertFalse(state.showDeleteDialog)
        assertFalse(state.isDeleting)
        assertFalse(state.deleteSuccess)
        assertNull(state.isOnImmich)
    }

    @Test
    fun `toggleHud flips isHudOpen`() {
        assertFalse(viewModel.state.value.isHudOpen)
        viewModel.toggleHud()
        assertTrue(viewModel.state.value.isHudOpen)
        viewModel.toggleHud()
        assertFalse(viewModel.state.value.isHudOpen)
    }

    @Test
    fun `setCurrentIndex updates currentIndex`() {
        viewModel.setCurrentIndex(5)
        assertEquals(5, viewModel.state.value.currentIndex)
    }

    @Test
    fun `setPinnedId stores pinned id`() {
        viewModel.setPinnedId("photo_123")
        assertEquals("photo_123", viewModel.state.value.pinnedId)
    }

    @Test
    fun `setPinnedId null clears pinned id`() {
        viewModel.setPinnedId("photo_123")
        viewModel.setPinnedId(null)
        assertNull(viewModel.state.value.pinnedId)
    }

    @Test
    fun `setLongPressing updates isLongPressing`() {
        viewModel.setLongPressing(true)
        assertTrue(viewModel.state.value.isLongPressing)
        viewModel.setLongPressing(false)
        assertFalse(viewModel.state.value.isLongPressing)
    }

    @Test
    fun `setLoading updates isLoading`() {
        viewModel.setLoading(false)
        assertFalse(viewModel.state.value.isLoading)
        viewModel.setLoading(true)
        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `toggleMarked adds photo id to markedIds`() {
        viewModel.toggleMarked("photo_1")
        assertTrue(viewModel.state.value.markedIds.contains("photo_1"))
        assertEquals(1, viewModel.state.value.markedIds.size)
        assertTrue(viewModel.state.value.isMarked)
    }

    @Test
    fun `toggleMarked removes photo id from markedIds`() {
        viewModel.toggleMarked("photo_1")
        viewModel.toggleMarked("photo_1")
        assertFalse(viewModel.state.value.markedIds.contains("photo_1"))
        assertTrue(viewModel.state.value.markedIds.isEmpty())
        assertFalse(viewModel.state.value.isMarked)
    }

    @Test
    fun `toggleMarked handles multiple photos independently`() {
        viewModel.toggleMarked("photo_1")
        viewModel.toggleMarked("photo_2")
        assertEquals(2, viewModel.state.value.markedIds.size)
        viewModel.toggleMarked("photo_1")
        assertEquals(1, viewModel.state.value.markedIds.size)
        assertTrue(viewModel.state.value.markedIds.contains("photo_2"))
        assertFalse(viewModel.state.value.isMarked)
    }

    @Test
    fun `setMarkedIds replaces all marked ids`() {
        viewModel.toggleMarked("photo_1")
        viewModel.setMarkedIds(setOf("photo_2", "photo_3"))
        assertEquals(2, viewModel.state.value.markedIds.size)
        assertFalse(viewModel.state.value.markedIds.contains("photo_1"))
        assertTrue(viewModel.state.value.markedIds.contains("photo_2"))
        assertTrue(viewModel.state.value.markedIds.contains("photo_3"))
    }

    @Test
    fun `setIsMarked updates isMarked independently of markedIds`() {
        viewModel.setIsMarked(true)
        assertTrue(viewModel.state.value.isMarked)
        viewModel.setIsMarked(false)
        assertFalse(viewModel.state.value.isMarked)
    }

    @Test
    fun `showDeleteDialog sets showDeleteDialog to true`() {
        viewModel.showDeleteDialog()
        assertTrue(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `hideDeleteDialog sets showDeleteDialog to false`() {
        viewModel.showDeleteDialog()
        viewModel.hideDeleteDialog()
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `setDeleting updates isDeleting`() {
        viewModel.setDeleting(true)
        assertTrue(viewModel.state.value.isDeleting)
        viewModel.setDeleting(false)
        assertFalse(viewModel.state.value.isDeleting)
    }

    @Test
    fun `setDeleteSuccess updates deleteSuccess`() {
        viewModel.setDeleteSuccess(true)
        assertTrue(viewModel.state.value.deleteSuccess)
        viewModel.setDeleteSuccess(false)
        assertFalse(viewModel.state.value.deleteSuccess)
    }

    @Test
    fun `clearMarkedIds clears all marks and hides dialog`() {
        viewModel.toggleMarked("photo_1")
        viewModel.showDeleteDialog()
        viewModel.clearMarkedIds()
        assertTrue(viewModel.state.value.markedIds.isEmpty())
        assertFalse(viewModel.state.value.isMarked)
        assertFalse(viewModel.state.value.showDeleteDialog)
    }

    @Test
    fun `setIsOnImmich updates isOnImmich`() {
        viewModel.setIsOnImmich(true)
        assertEquals(true, viewModel.state.value.isOnImmich)
        viewModel.setIsOnImmich(null)
        assertNull(viewModel.state.value.isOnImmich)
    }

    @Test
    fun `setPinnedId and toggleMarked work independently`() {
        viewModel.setPinnedId("pin_1")
        viewModel.toggleMarked("mark_1")
        assertEquals("pin_1", viewModel.state.value.pinnedId)
        assertTrue(viewModel.state.value.markedIds.contains("mark_1"))
    }

    @Test
    fun `multiple toggleMarked round trips maintain state`() {
        viewModel.toggleMarked("a")
        viewModel.toggleMarked("b")
        viewModel.toggleMarked("a")
        viewModel.toggleMarked("c")
        assertEquals(setOf("b", "c"), viewModel.state.value.markedIds)
    }
}
