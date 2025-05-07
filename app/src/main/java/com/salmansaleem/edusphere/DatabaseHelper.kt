package com.salmansaleem.edusphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EduSphere.db"
        private const val DATABASE_VERSION = 2 // Updated version
        private const val TABLE_USERS = "users"
        const val TABLE_PROFILE_UPDATES = "profile_updates"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        private const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        private const val COLUMN_PASSWORD = "password"

        const val COLUMN_UID = "uid"
        const val COLUMN_BIO = "bio"


        const val COLUMN_PROFILE_IMAGE_PATH = "profile_image_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_UID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT,
                $COLUMN_EMAIL TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_PASSWORD TEXT,
                $COLUMN_BIO TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        val createProfileUpdatesTable = """
            CREATE TABLE $TABLE_PROFILE_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_BIO TEXT,
                $COLUMN_PHONE TEXT,
                $COLUMN_PROFILE_IMAGE_PATH TEXT
            )
        """.trimIndent()
        db.execSQL(createProfileUpdatesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_BIO TEXT")
            db.execSQL("""
                CREATE TABLE $TABLE_PROFILE_UPDATES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_UID TEXT,
                    $COLUMN_NAME TEXT,
                    $COLUMN_BIO TEXT,
                    $COLUMN_PHONE TEXT,
                    $COLUMN_PROFILE_IMAGE_PATH TEXT
                )
            """.trimIndent())
        }
    }

    fun insertUser(name: String, email: String, phone: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PHONE, phone)
            put(COLUMN_PASSWORD, password)
        }

        // Check if email already exists
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_EMAIL),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null,
            null,
            null
        )
        val emailExists = cursor.moveToFirst()
        cursor.close()

        return if (emailExists) {
            // Update existing user
            val rowsAffected = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_EMAIL = ?",
                arrayOf(email)
            )
            db.close()
            rowsAffected > 0
        } else {
            // Insert new user
            val result = db.insert(TABLE_USERS, null, values)
            db.close()
            result != -1L
        }
    }

    fun verifyUser(email: String, password: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_NAME, COLUMN_EMAIL, COLUMN_PHONE, COLUMN_PASSWORD),
            "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(email, password),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val user = mapOf(
                COLUMN_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                COLUMN_EMAIL to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                COLUMN_PHONE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                COLUMN_PASSWORD to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            )
            cursor.close()
            db.close()
            user
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getAllUsers(): List<Map<String, String>> {
        val users = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_NAME, COLUMN_EMAIL, COLUMN_PHONE, COLUMN_PASSWORD),
            null,
            null,
            null,
            null,
            null
        )

        while (cursor.moveToNext()) {
            val user = mapOf(
                COLUMN_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                COLUMN_EMAIL to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                COLUMN_PHONE to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                COLUMN_PASSWORD to cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            )
            users.add(user)
        }
        cursor.close()
        db.close()
        return users
    }


    fun updateUserProfile(uid: String, name: String, bio: String, phone: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_BIO, bio)
            put(COLUMN_PHONE, phone)
        }

        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_UID = ?",
            arrayOf(uid)
        )

        return if (rowsAffected > 0) {
            true
        } else {
            values.put(COLUMN_UID, uid)
            db.insert(TABLE_USERS, null, values) > 0
        }
    }

    fun getUserProfile(uid: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_NAME, COLUMN_BIO, COLUMN_PHONE),
            "$COLUMN_UID = ?",
            arrayOf(uid),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val map = mutableMapOf<String, String>()
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["bio"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIO)) ?: ""
            map["phone"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)) ?: ""
            cursor.close()
            map
        } else {
            cursor.close()
            null
        }
    }

    fun queueProfileUpdate(uid: String, name: String, bio: String, phone: String, profileImagePath: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_BIO, bio)
            put(COLUMN_PHONE, phone)
            if (profileImagePath != null) {
                put(COLUMN_PROFILE_IMAGE_PATH, profileImagePath)
            }
        }
        db.insert(TABLE_PROFILE_UPDATES, null, values)
    }

}