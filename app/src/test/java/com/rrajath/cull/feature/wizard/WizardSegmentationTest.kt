package com.rrajath.cull.feature.wizard

import android.net.Uri
import com.rrajath.cull.core.model.UnifiedPhotoItem
import com.rrajath.cull.core.network.parseImmichTimestamp
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WizardSegmentationTest {

    private val utc = ZoneId.of("UTC")
    private val la = ZoneId.of("America/Los_Angeles")

    private fun epochMs(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: ZoneId): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun photo(id: String, dateTaken: Long) = UnifiedPhotoItem(
        id = id,
        uri = mockk<Uri>(),
        name = "$id.jpg",
        dateModified = dateTaken,
        dateTaken = dateTaken,
    )

    @Test
    fun `monthKey formats year and month with padding`() {
        assertEquals("2026-01", WizardSegmentation.monthKey(epochMs(2026, 1, 15, 12, 0, utc), utc))
        assertEquals("2025-12", WizardSegmentation.monthKey(epochMs(2025, 12, 1, 0, 0, utc), utc))
    }

    @Test
    fun `monthKey respects timezone at month boundary`() {
        // Jan 1 2026 03:00 UTC is still Dec 31 2025 19:00 in Los Angeles
        val instant = epochMs(2026, 1, 1, 3, 0, utc)
        assertEquals("2026-01", WizardSegmentation.monthKey(instant, utc))
        assertEquals("2025-12", WizardSegmentation.monthKey(instant, la))
    }

    @Test
    fun `countByMonth buckets by dateTaken`() {
        val photos = listOf(
            photo("1", epochMs(2026, 1, 5, 10, 0, utc)),
            photo("2", epochMs(2026, 1, 20, 10, 0, utc)),
            photo("3", epochMs(2025, 12, 31, 10, 0, utc)),
        )
        val counts = WizardSegmentation.countByMonth(photos, utc)
        assertEquals(mapOf("2026-01" to 2, "2025-12" to 1), counts)
    }

    @Test
    fun `monthBounds are inclusive start exclusive end`() {
        val (start, end) = WizardSegmentation.monthBounds("2026-01", utc)
        assertEquals(epochMs(2026, 1, 1, 0, 0, utc), start)
        assertEquals(epochMs(2026, 2, 1, 0, 0, utc), end)

        // last ms of January is inside; first ms of February is not
        assertEquals("2026-01", WizardSegmentation.monthKey(end - 1, utc))
        assertEquals("2026-02", WizardSegmentation.monthKey(end, utc))
    }

    @Test
    fun `monthBounds handle leap February`() {
        val (start, end) = WizardSegmentation.monthBounds("2024-02", utc)
        val days = (end - start) / (24 * 60 * 60 * 1000L)
        assertEquals(29, days)
    }

    @Test
    fun `monthTitle and abbreviation format correctly`() {
        assertEquals("January 2026", WizardSegmentation.monthTitle("2026-01"))
        assertEquals("Jan", WizardSegmentation.monthAbbreviation("2026-01"))
        assertEquals(2026, WizardSegmentation.year("2026-01"))
    }

    @Test
    fun `parseImmichTimestamp handles offset format`() {
        val ms = parseImmichTimestamp("2024-01-15T10:30:00.000+00:00")
        assertEquals(epochMs(2024, 1, 15, 10, 30, utc), ms)
    }

    @Test
    fun `parseImmichTimestamp handles Z suffix`() {
        val ms = parseImmichTimestamp("2024-01-15T10:30:00.000Z")
        assertEquals(epochMs(2024, 1, 15, 10, 30, utc), ms)
    }

    @Test
    fun `parseImmichTimestamp falls through garbage to next candidate`() {
        val ms = parseImmichTimestamp("not-a-date", null, "2024-01-15T10:30:00.000Z")
        assertEquals(epochMs(2024, 1, 15, 10, 30, utc), ms)
    }

    @Test
    fun `parseImmichTimestamp returns zero when nothing parses`() {
        assertEquals(0L, parseImmichTimestamp("garbage", null, ""))
    }
}
