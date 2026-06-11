package com.rrajath.occullt.feature.wizard

import com.rrajath.occullt.core.model.UnifiedPhotoItem
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure month-segmentation helpers for the Wizard. Month keys are "yyyy-MM"
 * (e.g. "2026-01") and bucketing always uses [UnifiedPhotoItem.dateTaken].
 */
object WizardSegmentation {

    fun monthKey(epochMs: Long, zone: ZoneId): String {
        val yearMonth = YearMonth.from(Instant.ofEpochMilli(epochMs).atZone(zone))
        return "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
    }

    fun countByMonth(photos: List<UnifiedPhotoItem>, zone: ZoneId): Map<String, Int> {
        return photos
            .groupingBy { monthKey(it.dateTaken, zone) }
            .eachCount()
    }

    /** Epoch-ms bounds of the month: [startInclusive, endExclusive). */
    fun monthBounds(monthKey: String, zone: ZoneId): Pair<Long, Long> {
        val yearMonth = YearMonth.parse(monthKey)
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    /** "2026-01" → "January 2026" */
    fun monthTitle(monthKey: String): String {
        val yearMonth = YearMonth.parse(monthKey)
        val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.US)
        return "$month ${yearMonth.year}"
    }

    /** "2026-01" → "Jan" */
    fun monthAbbreviation(monthKey: String): String {
        return YearMonth.parse(monthKey).month.getDisplayName(TextStyle.SHORT, Locale.US)
    }

    /** "2026-01" → 2026 */
    fun year(monthKey: String): Int = YearMonth.parse(monthKey).year
}
