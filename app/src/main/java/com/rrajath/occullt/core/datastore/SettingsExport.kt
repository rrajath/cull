package com.rrajath.occullt.core.datastore

import com.rrajath.occullt.ui.component.SourceMode
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
) {
    /**
     * Returns a copy safe to persist: unknown source modes fall back to the
     * default and numeric fields are clamped to the ranges the settings UI
     * allows. A crafted or corrupted import file must not be able to put the
     * app into a crash loop (e.g. SourceMode.valueOf throwing on every read).
     */
    fun sanitized(): SettingsExport = copy(
        sourceMode = (SourceMode.entries.find { it.name == sourceMode } ?: SourceMode.Hybrid).name,
        longPressThresholdMs = longPressThresholdMs.coerceIn(80, 800),
        accentHue = accentHue.coerceIn(0, 7),
        groupingWindowMinutes = groupingWindowMinutes.coerceIn(1, 30),
    )
}
