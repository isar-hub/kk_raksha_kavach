package com.isar.kkrakshakavach.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DbClassHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val TABLE_NAME = "contacts_data"
        const val COL_ID = "ID"
        const val COL_NAME = "NAME"
        const val COL_PHONE = "PHONE"
        const val COL_EMAIL = "EMAIL" // Optional: Add email field
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT,
                $COL_PHONE TEXT,
                $COL_EMAIL TEXT
            )
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Add data to the database
    fun addData(name: String, phone: String, email: String?): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL_NAME, name)
        contentValues.put(COL_PHONE, phone)
        contentValues.put(COL_EMAIL, email)

        // Insert data and return true if successful, false if failure
        val result = db.insert(TABLE_NAME, null, contentValues)
        return result != -1L
    }
    fun getAllContacts(): Cursor {
        val db = this.writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }
    fun getContactById(id: Int): Cursor? {
        val db = this.writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME WHERE $COL_ID = ?", arrayOf(id.toString()))
    }

    // Optional: Method to delete a contact
    fun deleteContact(id: Int): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }
}
