package com.rrajath.occullt.core.grouping

import android.net.Uri
import com.rrajath.occullt.core.model.UnifiedPhotoItem
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGroupingTest {

    private fun photo(id: String, dateModified: Long, dateTaken: Long = dateModified) =
        UnifiedPhotoItem(
            id = id,
            uri = mockk<Uri>(),
            name = "$id.jpg",
            dateModified = dateModified,
            dateTaken = dateTaken,
        )

    private val minute = 60_000L

    @Test
    fun `empty list returns no stacks`() {
        assertTrue(groupPhotos(emptyList(), 2 * minute).isEmpty())
    }

    @Test
    fun `single photo returns no stacks`() {
        assertTrue(groupPhotos(listOf(photo("1", 0)), 2 * minute).isEmpty())
    }

    @Test
    fun `singleton groups are dropped`() {
        val photos = listOf(
            photo("1", 0),
            photo("2", minute),
            // 10 minutes later, alone
            photo("3", 11 * minute),
        )
        val stacks = groupPhotos(photos, 2 * minute)
        assertEquals(1, stacks.size)
        assertEquals(listOf("1", "2"), stacks[0].photos.map { it.id })
    }

    @Test
    fun `photo exactly at window boundary joins the group`() {
        val photos = listOf(
            photo("1", 0),
            photo("2", 2 * minute),
        )
        val stacks = groupPhotos(photos, 2 * minute)
        assertEquals(1, stacks.size)
        assertEquals(2, stacks[0].photos.size)
    }

    @Test
    fun `window is anchored at first photo of group not previous photo`() {
        val photos = listOf(
            photo("1", 0),
            photo("2", 90_000L),
            // within 2 min of photo 2 but not of photo 1, so starts a new group
            photo("3", 180_000L),
            photo("4", 200_000L),
        )
        val stacks = groupPhotos(photos, 2 * minute)
        assertEquals(2, stacks.size)
    }

    @Test
    fun `stacks are returned newest first`() {
        val photos = listOf(
            photo("1", 0),
            photo("2", minute),
            photo("3", 60 * minute),
            photo("4", 61 * minute),
        )
        val stacks = groupPhotos(photos, 2 * minute)
        assertEquals(2, stacks.size)
        assertEquals("3", stacks[0].photos.first().id)
        assertEquals("1", stacks[1].photos.first().id)
    }

    @Test
    fun `start and end time come from group bounds`() {
        val photos = listOf(
            photo("1", 1_000L),
            photo("2", 50_000L),
        )
        val stacks = groupPhotos(photos, 2 * minute)
        assertEquals(1_000L, stacks[0].startTime)
        assertEquals(50_000L, stacks[0].endTime)
    }

    @Test
    fun `stackKey is stable for same photos and differs when composition changes`() {
        val photos = listOf(photo("1", 0), photo("2", minute))
        val sameAgain = listOf(photo("1", 0), photo("2", minute))
        val different = listOf(photo("1", 0), photo("3", minute))

        assertEquals(stackKey(photos), stackKey(sameAgain))
        assertTrue(stackKey(photos) != stackKey(different))
        assertTrue(stackKey(photos) != stackKey(photos.take(1)))
    }

    @Test
    fun `custom timestamp selector groups by dateTaken`() {
        // dateModified would put these in one group; dateTaken spreads them apart
        val photos = listOf(
            photo("1", dateModified = 0, dateTaken = 0),
            photo("2", dateModified = minute, dateTaken = 30 * minute),
            photo("3", dateModified = minute + 1, dateTaken = 31 * minute),
        )
        val sortedByTaken = photos.sortedBy { it.dateTaken }
        val stacks = groupPhotos(sortedByTaken, 2 * minute) { it.dateTaken }
        assertEquals(1, stacks.size)
        assertEquals(listOf("2", "3"), stacks[0].photos.map { it.id })
        assertEquals(30 * minute, stacks[0].startTime)
        assertEquals(31 * minute, stacks[0].endTime)
    }
}
