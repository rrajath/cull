package com.rrajath.occullt.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WizardSegmentDbTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: WizardSegmentDb

    @Before
    fun setUp() {
        db = WizardSegmentDb.getInstance(context)
        db.clearAll()
    }

    @After
    fun tearDown() {
        db.clearAll()
    }

    @Test
    fun upsertPhotoCountsCreatesNotStartedRows() {
        db.upsertPhotoCounts(mapOf("2026-01" to 47, "2025-12" to 118))

        val all = db.getAll()
        assertEquals(2, all.size)
        // getAll is sorted newest-first by month key
        assertEquals("2026-01", all[0].monthKey)
        assertEquals(47, all[0].photoCount)
        assertEquals(WizardSegmentState.NOT_STARTED, all[0].state)
        assertEquals(0, all[0].deletedCount)
        assertNull(all[0].startedAt)
    }

    @Test
    fun upsertPhotoCountsPreservesStateAndStats() {
        db.upsertPhotoCounts(mapOf("2026-01" to 47))
        db.markInProgressIfNotStarted("2026-01", 1000L)
        db.incrementDeletedCount("2026-01", 5)

        db.upsertPhotoCounts(mapOf("2026-01" to 42))

        val segment = db.get("2026-01")
        assertNotNull(segment)
        assertEquals(42, segment!!.photoCount)
        assertEquals(WizardSegmentState.IN_PROGRESS, segment.state)
        assertEquals(5, segment.deletedCount)
        assertEquals(1000L, segment.startedAt)
    }

    @Test
    fun upsertPhotoCountsZeroesMissingMonths() {
        db.upsertPhotoCounts(mapOf("2026-01" to 47, "2025-12" to 118))
        db.markComplete("2025-12", 2000L)

        db.upsertPhotoCounts(mapOf("2026-01" to 47))

        val missing = db.get("2025-12")
        assertNotNull(missing)
        assertEquals(0, missing!!.photoCount)
        // state survives the zero-out
        assertEquals(WizardSegmentState.COMPLETE, missing.state)
    }

    @Test
    fun markInProgressTransitionsOnlyFromNotStarted() {
        db.markInProgressIfNotStarted("2026-01", 1000L)

        val segment = db.get("2026-01")
        assertEquals(WizardSegmentState.IN_PROGRESS, segment!!.state)
        assertEquals(1000L, segment.startedAt)
        assertEquals(1000L, segment.lastAccessedAt)

        // re-entering does not reset startedAt
        db.markInProgressIfNotStarted("2026-01", 9999L)
        assertEquals(1000L, db.get("2026-01")!!.startedAt)
    }

    @Test
    fun completeIsTerminal() {
        db.markInProgressIfNotStarted("2026-01", 1000L)
        db.markComplete("2026-01", 2000L)

        db.markInProgressIfNotStarted("2026-01", 3000L)

        val segment = db.get("2026-01")
        assertEquals(WizardSegmentState.COMPLETE, segment!!.state)
        assertEquals(2000L, segment.completedAt)
    }

    @Test
    fun touchUpdatesOnlyLastAccessedAt() {
        db.markInProgressIfNotStarted("2026-01", 1000L)
        db.touch("2026-01", 5000L)

        val segment = db.get("2026-01")
        assertEquals(5000L, segment!!.lastAccessedAt)
        assertEquals(1000L, segment.startedAt)
        assertEquals(WizardSegmentState.IN_PROGRESS, segment.state)
    }

    @Test
    fun incrementDeletedCountAccumulates() {
        db.incrementDeletedCount("2026-01", 3)
        db.incrementDeletedCount("2026-01", 4)

        assertEquals(7, db.get("2026-01")!!.deletedCount)
    }

    @Test
    fun getUnknownMonthReturnsNull() {
        assertNull(db.get("1999-01"))
    }
}
