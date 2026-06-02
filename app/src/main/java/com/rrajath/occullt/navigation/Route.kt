package com.rrajath.occullt.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Home : Route

    @Serializable
    data object Library : Route

    @Serializable
    data class Viewer(
        val photoIndex: Int = 0,
        val folderUri: String? = null
    ) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Stacks : Route

    @Serializable
    data class StackGrid(val stackIndex: Int) : Route
}
