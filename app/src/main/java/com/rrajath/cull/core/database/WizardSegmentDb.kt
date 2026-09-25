package com.rrajath.cull.core.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

enum class WizardSegmentState { NOT_STARTED, IN_PROGRESS, COMPLETE }

data class WizardSegment(
    val monthKey: String,
    val state: WizardSegmentState,
    val photoCount: Int,
    val deletedCount: Int,
    val startedAt: Long?,
    val completedAt: Long?,
    val lastAccessedAt: Long?,
)

interface WizardSegmentStore {
    fun getAll(): List<WizardSegment>
    fun get(monthKey: String): WizardSegment?

    /**
     * Updates photo counts from a fresh library scan. Months absent from
     * [counts] are zeroed (their state and stats are preserved). Never touches
     * state, deleted_count, or timestamps of existing rows.
     */
    fun upsertPhotoCounts(counts: Map<String, Int>)

    /** NOT_STARTED → IN_PROGRESS; no-op for IN_PROGRESS/COMPLETE rows. */
    fun markInProgressIfNotStarted(monthKey: String, nowMs: Long)

    /** Updates last_accessed_at only. */
    fun touch(monthKey: String, nowMs: Long)

    fun markComplete(monthKey: String, nowMs: Long)
    fun incrementDeletedCount(monthKey: String, delta: Int)
}

class WizardSegmentDb(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), WizardSegmentStore {

    companion object {
        private const val DATABASE_NAME = "wizard_segments.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "wizard_segments"
        private const val COLUMN_MONTH_KEY = "month_key"
        private const val COLUMN_STATE = "state"
        private const val COLUMN_PHOTO_COUNT = "photo_count"
        private const val COLUMN_DELETED_COUNT = "deleted_count"
        private const val COLUMN_STARTED_AT = "started_at"
        private const val COLUMN_COMPLETED_AT = "completed_at"
        private const val COLUMN_LAST_ACCESSED_AT = "last_accessed_at"

        @Volatile
        private var INSTANCE: WizardSegmentDb? = null

        fun getInstance(context: Context): WizardSegmentDb {
            return INSTANCE ?: synchronized(this) {
                val instance = WizardSegmentDb(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_MONTH_KEY TEXT PRIMARY KEY,
                $COLUMN_STATE TEXT NOT NULL DEFAULT '${WizardSegmentState.NOT_STARTED.name}',
                $COLUMN_PHOTO_COUNT INTEGER NOT NULL DEFAULT 0,
                $COLUMN_DELETED_COUNT INTEGER NOT NULL DEFAULT 0,
                $COLUMN_STARTED_AT INTEGER,
                $COLUMN_COMPLETED_AT INTEGER,
                $COLUMN_LAST_ACCESSED_AT INTEGER
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    override fun getAll(): List<WizardSegment> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null, null, null, null, null,
            "$COLUMN_MONTH_KEY DESC"
        )
        return cursor.use {
            val segments = mutableListOf<WizardSegment>()
            while (it.moveToNext()) {
                segments.add(it.toSegment())
            }
            segments
        }
    }

    override fun get(monthKey: String): WizardSegment? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COLUMN_MONTH_KEY = ?", arrayOf(monthKey),
            null, null, null, "1"
        )
        return cursor.use {
            if (it.moveToFirst()) it.toSegment() else null
        }
    }

    override fun upsertPhotoCounts(counts: Map<String, Int>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((monthKey, count) in counts) {
                db.execSQL(
                    """
                    INSERT INTO $TABLE_NAME ($COLUMN_MONTH_KEY, $COLUMN_PHOTO_COUNT)
                    VALUES (?, ?)
                    ON CONFLICT($COLUMN_MONTH_KEY) DO UPDATE SET $COLUMN_PHOTO_COUNT = excluded.$COLUMN_PHOTO_COUNT
                    """.trimIndent(),
                    arrayOf(monthKey, count)
                )
            }
            if (counts.isEmpty()) {
                db.execSQL("UPDATE $TABLE_NAME SET $COLUMN_PHOTO_COUNT = 0")
            } else {
                val placeholders = counts.keys.joinToString(",") { "?" }
                db.execSQL(
                    "UPDATE $TABLE_NAME SET $COLUMN_PHOTO_COUNT = 0 WHERE $COLUMN_MONTH_KEY NOT IN ($placeholders)",
                    counts.keys.toTypedArray()
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun markInProgressIfNotStarted(monthKey: String, nowMs: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict(
                TABLE_NAME, null,
                ContentValues().apply {
                    put(COLUMN_MONTH_KEY, monthKey)
                    put(COLUMN_STATE, WizardSegmentState.NOT_STARTED.name)
                },
                SQLiteDatabase.CONFLICT_IGNORE
            )
            db.execSQL(
                """
                UPDATE $TABLE_NAME
                SET $COLUMN_STATE = '${WizardSegmentState.IN_PROGRESS.name}',
                    $COLUMN_STARTED_AT = ?,
                    $COLUMN_LAST_ACCESSED_AT = ?
                WHERE $COLUMN_MONTH_KEY = ? AND $COLUMN_STATE = '${WizardSegmentState.NOT_STARTED.name}'
                """.trimIndent(),
                arrayOf(nowMs, nowMs, monthKey)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun touch(monthKey: String, nowMs: Long) {
        writableDatabase.execSQL(
            "UPDATE $TABLE_NAME SET $COLUMN_LAST_ACCESSED_AT = ? WHERE $COLUMN_MONTH_KEY = ?",
            arrayOf(nowMs, monthKey)
        )
    }

    override fun markComplete(monthKey: String, nowMs: Long) {
        writableDatabase.execSQL(
            """
            UPDATE $TABLE_NAME
            SET $COLUMN_STATE = '${WizardSegmentState.COMPLETE.name}',
                $COLUMN_COMPLETED_AT = ?
            WHERE $COLUMN_MONTH_KEY = ?
            """.trimIndent(),
            arrayOf(nowMs, monthKey)
        )
    }

    override fun incrementDeletedCount(monthKey: String, delta: Int) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict(
                TABLE_NAME, null,
                ContentValues().apply { put(COLUMN_MONTH_KEY, monthKey) },
                SQLiteDatabase.CONFLICT_IGNORE
            )
            db.execSQL(
                "UPDATE $TABLE_NAME SET $COLUMN_DELETED_COUNT = $COLUMN_DELETED_COUNT + ? WHERE $COLUMN_MONTH_KEY = ?",
                arrayOf(delta, monthKey)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_NAME, null, null)
    }

    private fun Cursor.toSegment(): WizardSegment {
        fun longOrNull(column: String): Long? {
            val index = getColumnIndexOrThrow(column)
            return if (isNull(index)) null else getLong(index)
        }
        return WizardSegment(
            monthKey = getString(getColumnIndexOrThrow(COLUMN_MONTH_KEY)),
            state = WizardSegmentState.valueOf(getString(getColumnIndexOrThrow(COLUMN_STATE))),
            photoCount = getInt(getColumnIndexOrThrow(COLUMN_PHOTO_COUNT)),
            deletedCount = getInt(getColumnIndexOrThrow(COLUMN_DELETED_COUNT)),
            startedAt = longOrNull(COLUMN_STARTED_AT),
            completedAt = longOrNull(COLUMN_COMPLETED_AT),
            lastAccessedAt = longOrNull(COLUMN_LAST_ACCESSED_AT),
        )
    }
}
