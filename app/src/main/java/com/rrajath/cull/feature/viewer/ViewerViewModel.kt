package com.rrajath.cull.feature.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ViewerState(
    val currentIndex: Int = 0,
    val isHudOpen: Boolean = false,
    val pinnedId: String? = null,
    val isLongPressing: Boolean = false,
    val isLoading: Boolean = true,
    val markedIds: Set<String> = emptySet(),
    val isMarked: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteSuccess: Boolean = false,
    val isOnImmich: Boolean? = null,
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

    fun setPinnedId(id: String?) {
        _state.value = _state.value.copy(pinnedId = id)
    }

    fun setLongPressing(pressing: Boolean) {
        _state.value = _state.value.copy(isLongPressing = pressing)
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

    fun setIsOnImmich(onImmich: Boolean?) {
        _state.value = _state.value.copy(isOnImmich = onImmich)
    }
}
