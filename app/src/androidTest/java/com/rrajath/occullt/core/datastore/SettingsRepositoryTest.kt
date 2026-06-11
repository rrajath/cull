package com.rrajath.occullt.core.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rrajath.occullt.ui.component.SourceMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        context.filesDir.resolve("datastore").mkdirs()
        repo = SettingsRepository(context)
    }

    @After
    fun tearDown() = runBlocking {
        repo.setSourceMode(SourceMode.Hybrid)
        repo.setLibraryFolderUri("")
        repo.setImmichUrl("")
        repo.setImmichApiKey("")
        repo.setLongPressThreshold(220)
        repo.setDryRun(false)
        repo.setMirrorDeletes(false)
        repo.setDarkTheme(true)
        repo.setAccentHue(0)
        repo.setLastPhotoIndex(0, "")
        repo.setMarkedIds(emptySet())
        repo.setPinnedId(null)
        repo.setGroupingWindowMinutes(2)
        repo.setDoneStackKeys(emptySet())
    }

    @Test
    fun doneStackKeysRoundTrip() = runBlocking {
        assertTrue(repo.doneStackKeys.first().isEmpty())

        repo.addDoneStackKey("abc123")
        repo.addDoneStackKey("def456")
        assertEquals(setOf("abc123", "def456"), repo.doneStackKeys.first())

        // adding an existing key is idempotent
        repo.addDoneStackKey("abc123")
        assertEquals(setOf("abc123", "def456"), repo.doneStackKeys.first())

        repo.setDoneStackKeys(emptySet())
        assertTrue(repo.doneStackKeys.first().isEmpty())
    }

    @Test
    fun sourceModeDefaultsToHybrid() = runBlocking {
        val mode = repo.sourceMode.first()
        assertEquals(SourceMode.Hybrid, mode)
    }

    @Test
    fun setAndReadSourceMode() = runBlocking {
        repo.setSourceMode(SourceMode.Local)
        assertEquals(SourceMode.Local, repo.sourceMode.first())
        repo.setSourceMode(SourceMode.Immich)
        assertEquals(SourceMode.Immich, repo.sourceMode.first())
    }

    @Test
    fun setAndReadLibraryFolderUri() = runBlocking {
        val uri = "content://media/external/file/123"
        repo.setLibraryFolderUri(uri)
        assertEquals(uri, repo.libraryFolderUri.first())
    }

    @Test
    fun clearLibraryFolderUriSetsEmpty() = runBlocking {
        repo.setLibraryFolderUri("content://some/uri")
        repo.setLibraryFolderUri("")
        val value = repo.libraryFolderUri.first()
        assertTrue(value == null || value == "")
    }

    @Test
    fun setAndReadImmichUrl() = runBlocking {
        val url = "https://immich.example.com"
        repo.setImmichUrl(url)
        assertEquals(url, repo.immichUrl.first())
    }

    @Test
    fun setAndReadImmichApiKey() = runBlocking {
        val key = "test-api-key-123"
        repo.setImmichApiKey(key)
        assertEquals(key, repo.immichApiKey.first())
    }

    @Test
    fun longPressThresholdDefaultsTo220() = runBlocking {
        assertEquals(220, repo.longPressThreshold.first())
    }

    @Test
    fun setAndReadLongPressThreshold() = runBlocking {
        repo.setLongPressThreshold(500)
        assertEquals(500, repo.longPressThreshold.first())
    }

    @Test
    fun dryRunDefaultsToFalse() = runBlocking {
        assertFalse(repo.dryRun.first())
    }

    @Test
    fun setAndReadDryRun() = runBlocking {
        repo.setDryRun(true)
        assertTrue(repo.dryRun.first())
    }

    @Test
    fun mirrorDeletesDefaultsToFalse() = runBlocking {
        assertFalse(repo.mirrorDeletes.first())
    }

    @Test
    fun setAndReadMirrorDeletes() = runBlocking {
        repo.setMirrorDeletes(true)
        assertTrue(repo.mirrorDeletes.first())
    }

    @Test
    fun darkThemeDefaultsToTrue() = runBlocking {
        assertTrue(repo.darkTheme.first())
    }

    @Test
    fun setAndReadDarkTheme() = runBlocking {
        repo.setDarkTheme(false)
        assertFalse(repo.darkTheme.first())
    }

    @Test
    fun accentHueDefaultsToZero() = runBlocking {
        assertEquals(0, repo.accentHue.first())
    }

    @Test
    fun setAndReadAccentHue() = runBlocking {
        repo.setAccentHue(3)
        assertEquals(3, repo.accentHue.first())
    }

    @Test
    fun accentHueClampsToValidRange() = runBlocking {
        repo.setAccentHue(10)
        assertEquals(0, repo.accentHue.first())
    }

    @Test
    fun lastPhotoIndexDefaultsToZero() = runBlocking {
        assertEquals(0, repo.lastPhotoIndex.first())
    }

    @Test
    fun setAndReadLastPhotoIndex() = runBlocking {
        repo.setLastPhotoIndex(42, "content://folder")
        assertEquals(42, repo.lastPhotoIndex.first())
        assertEquals("content://folder", repo.lastFolderUri.first())
    }

    @Test
    fun markedIdsDefaultsToEmpty() = runBlocking {
        assertTrue(repo.markedIds.first().isEmpty())
    }

    @Test
    fun setAndReadMarkedIds() = runBlocking {
        val ids = setOf("photo1", "photo2", "photo3")
        repo.setMarkedIds(ids)
        assertEquals(ids, repo.markedIds.first())
    }

    @Test
    fun setMarkedIdsEmpty() = runBlocking {
        repo.setMarkedIds(setOf("photo1"))
        repo.setMarkedIds(emptySet())
        assertTrue(repo.markedIds.first().isEmpty())
    }

    @Test
    fun pinnedIdDefaultsToNull() = runBlocking {
        assertNull(repo.pinnedId.first())
    }

    @Test
    fun setAndReadPinnedId() = runBlocking {
        repo.setPinnedId("photo_1")
        assertEquals("photo_1", repo.pinnedId.first())
    }

    @Test
    fun setPinnedIdToNullClearsIt() = runBlocking {
        repo.setPinnedId("photo_1")
        repo.setPinnedId(null)
        assertNull(repo.pinnedId.first())
    }

    @Test
    fun groupingWindowMinutesDefaultsTo2() = runBlocking {
        assertEquals(2, repo.groupingWindowMinutes.first())
    }

    @Test
    fun setAndReadGroupingWindowMinutes() = runBlocking {
        repo.setGroupingWindowMinutes(10)
        assertEquals(10, repo.groupingWindowMinutes.first())
    }
}
