package com.rrajath.occullt.feature.wizard

import com.rrajath.occullt.core.database.WizardSegment
import com.rrajath.occullt.core.database.WizardSegmentState
import com.rrajath.occullt.core.database.WizardSegmentStore

/** In-memory store mirroring WizardSegmentDb's SQL semantics. */
class FakeWizardSegmentStore : WizardSegmentStore {
    private val map = linkedMapOf<String, WizardSegment>()

    fun seed(segment: WizardSegment) {
        map[segment.monthKey] = segment
    }

    private fun blank(monthKey: String) = WizardSegment(
        monthKey = monthKey,
        state = WizardSegmentState.NOT_STARTED,
        photoCount = 0,
        deletedCount = 0,
        startedAt = null,
        completedAt = null,
        lastAccessedAt = null,
    )

    override fun getAll(): List<WizardSegment> =
        map.values.sortedByDescending { it.monthKey }

    override fun get(monthKey: String): WizardSegment? = map[monthKey]

    override fun upsertPhotoCounts(counts: Map<String, Int>) {
        for ((monthKey, count) in counts) {
            val existing = map[monthKey]
            map[monthKey] = existing?.copy(photoCount = count)
                ?: blank(monthKey).copy(photoCount = count)
        }
        map.keys.filter { it !in counts }.forEach { monthKey ->
            map[monthKey] = map.getValue(monthKey).copy(photoCount = 0)
        }
    }

    override fun markInProgressIfNotStarted(monthKey: String, nowMs: Long) {
        val existing = map[monthKey] ?: blank(monthKey)
        map[monthKey] = if (existing.state == WizardSegmentState.NOT_STARTED) {
            existing.copy(
                state = WizardSegmentState.IN_PROGRESS,
                startedAt = nowMs,
                lastAccessedAt = nowMs,
            )
        } else {
            existing
        }
    }

    override fun touch(monthKey: String, nowMs: Long) {
        map[monthKey]?.let { map[monthKey] = it.copy(lastAccessedAt = nowMs) }
    }

    override fun markComplete(monthKey: String, nowMs: Long) {
        map[monthKey]?.let {
            map[monthKey] = it.copy(state = WizardSegmentState.COMPLETE, completedAt = nowMs)
        }
    }

    override fun incrementDeletedCount(monthKey: String, delta: Int) {
        val existing = map[monthKey] ?: blank(monthKey)
        map[monthKey] = existing.copy(deletedCount = existing.deletedCount + delta)
    }
}
