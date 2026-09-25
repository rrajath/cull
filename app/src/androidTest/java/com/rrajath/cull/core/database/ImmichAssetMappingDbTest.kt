package com.rrajath.cull.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImmichAssetMappingDbTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: ImmichAssetMappingDb

    @Before
    fun setUp() {
        context.getDatabasePath("test.db").parentFile?.mkdirs()
        db = ImmichAssetMappingDb.getInstance(context)
        db.clearAll()
    }

    @After
    fun tearDown() {
        db.clearAll()
    }

    @Test
    fun insertAndQueryByFileName() {
        val mapping = ImmichAssetMapping(
            id = "asset_1",
            originalFileName = "photo1.jpg",
            fileCreatedAt = "2024-01-01T00:00:00.000Z",
            fileModifiedAt = "2024-01-01T00:00:00.000Z",
        )
        db.insertAll(listOf(mapping))

        val result = db.getByFileName("photo1.jpg")
        assertNotNull(result)
        assertEquals("asset_1", result?.id)
        assertEquals("photo1.jpg", result?.originalFileName)
    }

    @Test
    fun queryNonExistentFileReturnsNull() {
        val result = db.getByFileName("nonexistent.jpg")
        assertNull(result)
    }

    @Test
    fun clearAllRemovesAllMappings() {
        val mapping = ImmichAssetMapping(
            id = "asset_1",
            originalFileName = "photo1.jpg",
            fileCreatedAt = "2024-01-01T00:00:00.000Z",
            fileModifiedAt = "2024-01-01T00:00:00.000Z",
        )
        db.insertAll(listOf(mapping))
        db.clearAll()

        val result = db.getByFileName("photo1.jpg")
        assertNull(result)
    }

    @Test
    fun insertMultipleMappings() {
        val mappings = listOf(
            ImmichAssetMapping(
                id = "asset_1",
                originalFileName = "photo1.jpg",
                fileCreatedAt = "2024-01-01T00:00:00.000Z",
                fileModifiedAt = "2024-01-01T00:00:00.000Z",
            ),
            ImmichAssetMapping(
                id = "asset_2",
                originalFileName = "photo2.jpg",
                fileCreatedAt = "2024-01-01T00:00:00.000Z",
                fileModifiedAt = "2024-01-01T00:00:00.000Z",
            ),
        )
        db.insertAll(mappings)

        assertEquals("asset_1", db.getByFileName("photo1.jpg")?.id)
        assertEquals("asset_2", db.getByFileName("photo2.jpg")?.id)
    }

    @Test
    fun insertReplacesExistingMappingOnConflict() {
        val first = ImmichAssetMapping(
            id = "asset_1",
            originalFileName = "photo1.jpg",
            fileCreatedAt = "2024-01-01T00:00:00.000Z",
            fileModifiedAt = "2024-01-01T00:00:00.000Z",
        )
        db.insertAll(listOf(first))

        val updated = ImmichAssetMapping(
            id = "asset_1",
            originalFileName = "photo1.jpg",
            fileCreatedAt = "2024-06-01T00:00:00.000Z",
            fileModifiedAt = "2024-06-01T00:00:00.000Z",
        )
        db.insertAll(listOf(updated))

        val result = db.getByFileName("photo1.jpg")
        assertNotNull(result)
        assertEquals("asset_1", result?.id)
        assertEquals("2024-06-01T00:00:00.000Z", result?.fileCreatedAt)
    }

    @Test
    fun getInstanceReturnsSingleton() {
        val instance1 = ImmichAssetMappingDb.getInstance(context)
        val instance2 = ImmichAssetMappingDb.getInstance(context)
        assertTrue(instance1 === instance2)
    }
}
