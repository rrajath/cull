package com.rrajath.occullt.feature.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewerState(
    val currentIndex: Int = 0,
    val isHudOpen: Boolean = false,
    val pinnedIndex: Int? = null,
    val isLongPressing: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isLoading: Boolean = true,
    val markedIds: Set<String> = emptySet(),
    val isMarked: Boolean = false,
    val pinnedScale: Float = 1f,
    val pinnedOffsetX: Float = 0f,
    val pinnedOffsetY: Float = 0f,
    val longPressThreshold: Int = 220,
    val swipeOffsetY: Float = 0f,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
)

class ViewerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ViewerState())
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    fun setCurrentIndex(index: Int) {
        _state.value = _state.value.copy(currentIndex = index)
    }

    fun toggleHud() {
        _state.value = _state.value.copy(
            isHudOpen = !_state.value.isHudOpen
        )
    }

    fun setPinnedIndex(index: Int?) {
        _state.value = _state.value.copy(pinnedIndex = index)
    }

    fun setLongPressing(pressing: Boolean) {
        _state.value = _state.value.copy(isLongPressing = pressing)
    }

    fun setZoom(scale: Float, offsetX: Float, offsetY: Float) {
        _state.value = _state.value.copy(
            scale = scale,
            offsetX = offsetX,
            offsetY = offsetY,
            pinnedScale = scale,
            pinnedOffsetX = offsetX,
            pinnedOffsetY = offsetY,
        )
    }

    fun resetZoom() {
        _state.value = _state.value.copy(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            pinnedScale = 1f,
            pinnedOffsetX = 0f,
            pinnedOffsetY = 0f,
        )
    }

    fun setLoading(loading: Boolean) {
        _state.value = _state.value.copy(isLoading = loading)
    }

    fun setMarkedIds(ids: Set<String>) {
        _state.value = _state.value.copy(markedIds = ids)
    }

    fun toggleMarked(photoId: String) {
        val current = _state.value.markedIds
        val updated = if (current.contains(photoId)) {
            current - photoId
        } else {
            current + photoId
        }
        _state.value = _state.value.copy(
            markedIds = updated,
            isMarked = updated.contains(photoId)
        )
    }

    fun setIsMarked(marked: Boolean) {
        _state.value = _state.value.copy(isMarked = marked)
    }

    fun setLongPressThreshold(ms: Int) {
        _state.value = _state.value.copy(longPressThreshold = ms)
    }

    fun setSwipeOffsetY(offset: Float) {
        _state.value = _state.value.copy(swipeOffsetY = offset)
    }

    fun resetSwipeOffset() {
        _state.value = _state.value.copy(swipeOffsetY = 0f)
    }

    fun undoMark(photoId: String) {
        val current = _state.value.markedIds
        if (current.contains(photoId)) {
            _state.value = _state.value.copy(
                markedIds = current - photoId,
                isMarked = false,
                swipeOffsetY = 0f
            )
        }
    }

    fun showDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = true)
    }

    fun hideDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = false)
    }

    fun setDeleting(deleting: Boolean) {
        _state.value = _state.value.copy(isDeleting = deleting)
    }

    fun setDeleteSuccess(success: Boolean) {
        _state.value = _state.value.copy(deleteSuccess = success)
    }

    fun clearMarkedIds() {
        _state.value = _state.value.copy(
            markedIds = emptySet(),
            isMarked = false,
            showDeleteDialog = false
        )
    }
}
