package com.isar.kkrakshakavach.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DbClassHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    companion object {
        const val DATABASE_NAME = "mylist.db"
        const val TABLE_NAME = "mylist_data"
        const val COL1 = "ID"
        const val COL2 = "ITEM1"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL1 INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL2 TEXT
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Add data to the database
    fun addData(item1: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL2, item1)

        // Insert data and return true if successful, false if failure
        val result = db.insert(TABLE_NAME, null, contentValues)
        return result != -1L
    }

    // Retrieve all data from the database
    fun getListContents(): Cursor {
        val db = this.writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }
}
