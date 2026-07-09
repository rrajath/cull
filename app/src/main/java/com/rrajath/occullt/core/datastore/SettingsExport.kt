package com.rrajath.occullt.core.datastore

import kotlinx.serialization.Serializable

/**
 * Portable snapshot of user-configurable settings.
 *
 * Intentionally excludes the Immich API key (secret, must never leave the device) and the
 * library folder / Immich SAF-derived state (device-local, not portable across installs).
 */
@Serializable
data class SettingsExport(
    val version: Int = 1,
    val sourceMode: String,
    val immichUrl: String? = null,
    val longPressThresholdMs: Int,
    val dryRun: Boolean,
    val mirrorDeletes: Boolean,
    val darkTheme: Boolean,
    val accentHue: Int,
    val groupingWindowMinutes: Int,
)
