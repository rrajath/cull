package com.rrajath.cull.core.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ImmichAssetMappingDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "immich_asset_mapping.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "immich_asset_mapping"
        private const val COLUMN_ID = "id"
        private const val COLUMN_FILE_NAME = "originalFileName"
        private const val COLUMN_CREATED_AT = "fileCreatedAt"
        private const val COLUMN_MODIFIED_AT = "fileModifiedAt"

        @Volatile
        private var INSTANCE: ImmichAssetMappingDb? = null

        fun getInstance(context: Context): ImmichAssetMappingDb {
            return INSTANCE ?: synchronized(this) {
                val instance = ImmichAssetMappingDb(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_FILE_NAME TEXT NOT NULL,
                $COLUMN_CREATED_AT TEXT,
                $COLUMN_MODIFIED_AT TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX idx_filename ON $TABLE_NAME($COLUMN_FILE_NAME)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertAll(mappings: List<ImmichAssetMapping>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (mapping in mappings) {
                val values = ContentValues().apply {
                    put(COLUMN_ID, mapping.id)
                    put(COLUMN_FILE_NAME, mapping.originalFileName)
                    put(COLUMN_CREATED_AT, mapping.fileCreatedAt)
                    put(COLUMN_MODIFIED_AT, mapping.fileModifiedAt)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getByFileName(fileName: String): ImmichAssetMapping? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_ID, COLUMN_FILE_NAME, COLUMN_CREATED_AT, COLUMN_MODIFIED_AT),
            "$COLUMN_FILE_NAME = ?",
            arrayOf(fileName),
            null, null, null, "1"
        )
        return cursor.use {
            if (it.moveToFirst()) {
                ImmichAssetMapping(
                    id = it.getString(it.getColumnIndexOrThrow(COLUMN_ID)),
                    originalFileName = it.getString(it.getColumnIndexOrThrow(COLUMN_FILE_NAME)),
                    fileCreatedAt = it.getString(it.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    fileModifiedAt = it.getString(it.getColumnIndexOrThrow(COLUMN_MODIFIED_AT)),
                )
            } else {
                null
            }
        }
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NAME, null, null)
    }
}

data class ImmichAssetMapping(
    val id: String,
    val originalFileName: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
)
