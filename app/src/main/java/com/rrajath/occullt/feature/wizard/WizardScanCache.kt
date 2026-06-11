package com.rrajath.occullt.feature.wizard

import com.rrajath.occullt.core.model.UnifiedPhotoItem

/**
 * In-memory cache of the most recent full-library scan, shared between the
 * Wizard home (which performs the scan) and the month segment view (which
 * filters it by month).
 */
object WizardScanCache {
    private var cachedScan: List<UnifiedPhotoItem> = emptyList()

    fun setScan(photos: List<UnifiedPhotoItem>) {
        cachedScan = photos
    }

    fun getScan(): List<UnifiedPhotoItem>? {
        return if (cachedScan.isNotEmpty()) cachedScan else null
    }

    fun clear() {
        cachedScan = emptyList()
    }
}
