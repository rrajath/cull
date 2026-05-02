package com.rrajath.occullt.feature.viewer

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ViewerState(
    val currentIndex: Int = 0,
    val isHudOpen: Boolean = false,
    val pinnedIndex: Int? = null,
    val isLongPressing: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val isLoading: Boolean = true,
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
            offsetY = offsetY
        )
    }

    fun resetZoom() {
        _state.value = _state.value.copy(
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f
        )
    }

    fun setLoading(loading: Boolean) {
        _state.value = _state.value.copy(isLoading = loading)
    }
}
