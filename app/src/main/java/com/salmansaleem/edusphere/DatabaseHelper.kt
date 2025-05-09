package com.salmansaleem.edusphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EduSphere.db"
        private const val DATABASE_VERSION = 8 // Updated version for classroom support
        private const val TABLE_USERS = "users"
        const val TABLE_PROFILE_UPDATES = "profile_updates"
        private const val TABLE_CLASSROOMS = "classrooms"
        const val TABLE_CLASSROOM_UPDATES = "classroom_updates"
        private const val TABLE_CLASSES = "classes"
        private const val TABLE_ANNOUNCEMENTS = "announcements"
        const val TABLE_ANNOUNCEMENT_UPDATES = "announcement_updates"
        private const val TABLE_COMMENTS = "comments"
        const val TABLE_COMMENT_UPDATES = "comment_updates"


        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        private const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        private const val COLUMN_PASSWORD = "password"

        const val COLUMN_UID = "uid"
        const val COLUMN_BIO = "bio"


        const val COLUMN_PROFILE_IMAGE_PATH = "profile_image_path"


        // Classroom columns
        const val COLUMN_CLASSROOM_ID = "classroom_id"
        const val COLUMN_SECTION = "section"
        const val COLUMN_ROOM = "room"
        const val COLUMN_SUBJECT = "subject"
        const val COLUMN_CLASS_CODE = "class_code"
        const val COLUMN_CLASSROOM_IMAGE_PATH = "classroom_image_path"
        const val COLUMN_INSTRUCTOR_NAME = "instructor_name"

        const val COLUMN_MEMBER_UID = "member_uid"


        const val COLUMN_ANNOUNCEMENT_ID = "announcement_id"
        const val COLUMN_ANNOUNCEMENT_TEXT = "announcement_text"
        const val COLUMN_TIMESTAMP = "timestamp"



        const val COLUMN_COMMENT_ID = "comment_id"
        const val COLUMN_COMMENT_TEXT = "comment_text"
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

        // New classrooms table
        val createClassroomsTable = """
            CREATE TABLE $TABLE_CLASSROOMS (
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_SECTION TEXT,
                $COLUMN_ROOM TEXT,
                $COLUMN_SUBJECT TEXT,
                $COLUMN_CLASS_CODE TEXT,
                $COLUMN_CLASSROOM_IMAGE_PATH TEXT,
                $COLUMN_INSTRUCTOR_NAME TEXT
            )
        """.trimIndent()
        db.execSQL(createClassroomsTable)

        // New classroom updates table
        val createClassroomUpdatesTable = """
            CREATE TABLE $TABLE_CLASSROOM_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_SECTION TEXT,
                $COLUMN_ROOM TEXT,
                $COLUMN_SUBJECT TEXT,
                $COLUMN_CLASS_CODE TEXT,
                $COLUMN_CLASSROOM_IMAGE_PATH TEXT,
                $COLUMN_INSTRUCTOR_NAME TEXT
            )
        """.trimIndent()
        db.execSQL(createClassroomUpdatesTable)

        val createClassesTable = """
        CREATE TABLE $TABLE_CLASSES (
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_MEMBER_UID TEXT,
            PRIMARY KEY ($COLUMN_CLASSROOM_ID, $COLUMN_MEMBER_UID)
        )
    """.trimIndent()
        db.execSQL(createClassesTable)

        val createAnnouncementsTable = """
        CREATE TABLE $TABLE_ANNOUNCEMENTS (
            $COLUMN_ANNOUNCEMENT_ID TEXT PRIMARY KEY,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_NAME TEXT,
            $COLUMN_ANNOUNCEMENT_TEXT TEXT,
            $COLUMN_TIMESTAMP TEXT
        )
    """.trimIndent()
        db.execSQL(createAnnouncementsTable)

        val createAnnouncementUpdatesTable = """
        CREATE TABLE $TABLE_ANNOUNCEMENT_UPDATES (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_ANNOUNCEMENT_ID TEXT,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_NAME TEXT,
            $COLUMN_ANNOUNCEMENT_TEXT TEXT,
            $COLUMN_TIMESTAMP TEXT
        )
    """.trimIndent()
        db.execSQL(createAnnouncementUpdatesTable)


        val createCommentsTable = """
        CREATE TABLE $TABLE_COMMENTS (
            $COLUMN_COMMENT_ID TEXT PRIMARY KEY,
            $COLUMN_ANNOUNCEMENT_ID TEXT,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_NAME TEXT,
            $COLUMN_COMMENT_TEXT TEXT,
            $COLUMN_TIMESTAMP TEXT
        )
    """.trimIndent()
        db.execSQL(createCommentsTable)

        val createCommentUpdatesTable = """
        CREATE TABLE $TABLE_COMMENT_UPDATES (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_COMMENT_ID TEXT,
            $COLUMN_ANNOUNCEMENT_ID TEXT,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_NAME TEXT,
            $COLUMN_COMMENT_TEXT TEXT,
            $COLUMN_TIMESTAMP TEXT
        )
    """.trimIndent()
        db.execSQL(createCommentUpdatesTable)
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
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE $TABLE_CLASSROOMS (
                    $COLUMN_CLASSROOM_ID TEXT PRIMARY KEY,
                    $COLUMN_UID TEXT,
                    $COLUMN_NAME TEXT,
                    $COLUMN_SECTION TEXT,
                    $COLUMN_ROOM TEXT,
                    $COLUMN_SUBJECT TEXT,
                    $COLUMN_CLASS_CODE TEXT,
                    $COLUMN_CLASSROOM_IMAGE_PATH TEXT,
                    $COLUMN_INSTRUCTOR_NAME TEXT
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE $TABLE_CLASSROOM_UPDATES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_CLASSROOM_ID TEXT,
                    $COLUMN_UID TEXT,
                    $COLUMN_NAME TEXT,
                    $COLUMN_SECTION TEXT,
                    $COLUMN_ROOM TEXT,
                    $COLUMN_SUBJECT TEXT,
                    $COLUMN_CLASS_CODE TEXT,
                    $COLUMN_CLASSROOM_IMAGE_PATH TEXT,
                    $COLUMN_INSTRUCTOR_NAME TEXT
                )
            """.trimIndent())
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_CLASSROOMS ADD COLUMN $COLUMN_INSTRUCTOR_NAME TEXT")
            db.execSQL("ALTER TABLE $TABLE_CLASSROOM_UPDATES ADD COLUMN $COLUMN_INSTRUCTOR_NAME TEXT")
        }
        if (oldVersion < 5) {
            db.execSQL("""
            CREATE TABLE $TABLE_CLASSES (
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_MEMBER_UID TEXT,
                PRIMARY KEY ($COLUMN_CLASSROOM_ID, $COLUMN_MEMBER_UID)
            )
        """.trimIndent())
        }

        if (oldVersion < 6) {
            db.execSQL("""
            CREATE TABLE $TABLE_ANNOUNCEMENTS (
                $COLUMN_ANNOUNCEMENT_ID TEXT PRIMARY KEY,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_ANNOUNCEMENT_TEXT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent())
            db.execSQL("""
            CREATE TABLE $TABLE_ANNOUNCEMENT_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ANNOUNCEMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_ANNOUNCEMENT_TEXT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent())
        }
        if (oldVersion < 7) {
            db.execSQL("""
            CREATE TABLE $TABLE_COMMENTS (
                $COLUMN_COMMENT_ID TEXT PRIMARY KEY,
                $COLUMN_ANNOUNCEMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_COMMENT_TEXT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent())
            db.execSQL("""
            CREATE TABLE $TABLE_COMMENT_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COMMENT_ID TEXT,
                $COLUMN_ANNOUNCEMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_NAME TEXT,
                $COLUMN_COMMENT_TEXT TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent())
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_PROFILE_IMAGE_PATH TEXT")
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




    // New classroom methods
    // Classroom methods
    fun insertClassroom(
        classroomId: String,
        uid: String,
        name: String,
        section: String,
        room: String,
        subject: String,
        classCode: String,
        imagePath: String?,
        instructorName: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_SECTION, section)
            put(COLUMN_ROOM, room)
            put(COLUMN_SUBJECT, subject)
            put(COLUMN_CLASS_CODE, classCode)
            put(COLUMN_CLASSROOM_IMAGE_PATH, imagePath)
            put(COLUMN_INSTRUCTOR_NAME, instructorName)
        }
        val result = db.insert(TABLE_CLASSROOMS, null, values)
        db.close()
        return result != -1L
    }
    fun getClassroom(classroomId: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSROOMS,
            arrayOf(
                COLUMN_NAME,
                COLUMN_SECTION,
                COLUMN_ROOM,
                COLUMN_SUBJECT,
                COLUMN_CLASS_CODE,
                COLUMN_CLASSROOM_IMAGE_PATH,
                COLUMN_INSTRUCTOR_NAME
            ),
            "$COLUMN_CLASSROOM_ID = ?",
            arrayOf(classroomId),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val map = mutableMapOf<String, String>()
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["section"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
            map["room"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
            map["subject"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
            map["class_code"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
            map["image_path"] = if (imagePath.isNotEmpty()) imagePath else ""
            map["instructor_name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
            cursor.close()
            map
        } else {
            cursor.close()
            null
        }
    }

    fun queueClassroomUpdate(
        classroomId: String,
        uid: String,
        name: String,
        section: String,
        room: String,
        subject: String,
        classCode: String,
        imagePath: String?,
        instructorName: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_SECTION, section)
            put(COLUMN_ROOM, room)
            put(COLUMN_SUBJECT, subject)
            put(COLUMN_CLASS_CODE, classCode)
            put(COLUMN_CLASSROOM_IMAGE_PATH, imagePath)
            put(COLUMN_INSTRUCTOR_NAME, instructorName)
        }
        db.insert(TABLE_CLASSROOM_UPDATES, null, values)
        db.close()
    }

    fun getAllClassroomsForUser(uid: String): List<Map<String, String>> {
        val classrooms = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSROOMS,
            arrayOf(
                COLUMN_CLASSROOM_ID,
                COLUMN_NAME,
                COLUMN_SECTION,
                COLUMN_ROOM,
                COLUMN_SUBJECT,
                COLUMN_CLASS_CODE,
                COLUMN_CLASSROOM_IMAGE_PATH,
                COLUMN_INSTRUCTOR_NAME
            ),
            "$COLUMN_UID = ?",
            arrayOf(uid),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["section"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
            map["room"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
            map["subject"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
            map["class_code"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
            // Use local file path
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
            map["image_path"] = if (imagePath.isNotEmpty()) imagePath else ""
            map["instructor_name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
            classrooms.add(map)
        }
        cursor.close()
        db.close()
        return classrooms
    }



    fun updateUserProfileImage(uid: String, imagePath: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PROFILE_IMAGE_PATH, imagePath)
        }
        val rowsAffected = db.update(
            TABLE_USERS,
            values,
            "$COLUMN_UID = ?",
            arrayOf(uid)
        )
        db.close()
        return rowsAffected > 0
    }

    fun isClassMember(classroomId: String, memberUid: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSES,
            arrayOf(COLUMN_CLASSROOM_ID),
            "$COLUMN_CLASSROOM_ID = ? AND $COLUMN_MEMBER_UID = ?",
            arrayOf(classroomId, memberUid),
            null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        db.close()
        return exists
    }


    fun addClassMember(classroomId: String, memberUid: String): Boolean {

        if (isClassMember(classroomId, memberUid)) {
            return true // Member already exists, no need to insert
        }
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_MEMBER_UID, memberUid)
        }
        val result = db.insert(TABLE_CLASSES, null, values)
        db.close()
        return result != -1L
    }

    fun getUserClassrooms(uid: String): List<String> {
        val classroomIds = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSES,
            arrayOf(COLUMN_CLASSROOM_ID),
            "$COLUMN_MEMBER_UID = ?",
            arrayOf(uid),
            null,
            null,
            null
        )
        while (cursor.moveToNext()) {
            val classroomId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: continue
            classroomIds.add(classroomId)
        }
        cursor.close()
        db.close()
        return classroomIds
    }

    fun joinClassByCode(classCode: String, memberUid: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSROOMS,
            arrayOf(COLUMN_CLASSROOM_ID, COLUMN_NAME, COLUMN_SECTION, COLUMN_ROOM, COLUMN_SUBJECT, COLUMN_CLASS_CODE, COLUMN_CLASSROOM_IMAGE_PATH, COLUMN_INSTRUCTOR_NAME),
            "$COLUMN_CLASS_CODE = ?",
            arrayOf(classCode),
            null,
            null,
            null
        )
        val classroomId = if (cursor.moveToFirst()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            val section = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
            val room = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
            val subject = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
            val classCode = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
            val instructorName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
            // Insert classroom data into SQLite
            insertClassroom(id, memberUid, name, section, room, subject, classCode, imagePath, instructorName)
            id
        } else {
            null
        }
        cursor.close()
        if (classroomId != null) {
            addClassMember(classroomId, memberUid)
        }
        db.close()
        return classroomId
    }



    fun insertAnnouncement(
        announcementId: String,
        classroomId: String,
        uid: String,
        name: String,
        text: String,
        timestamp: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ANNOUNCEMENT_ID, announcementId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_ANNOUNCEMENT_TEXT, text)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        val result = db.insert(TABLE_ANNOUNCEMENTS, null, values)
        db.close()
        return result != -1L
    }

    fun queueAnnouncementUpdate(
        announcementId: String,
        classroomId: String,
        uid: String,
        name: String,
        text: String,
        timestamp: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ANNOUNCEMENT_ID, announcementId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_ANNOUNCEMENT_TEXT, text)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_ANNOUNCEMENT_UPDATES, null, values)
        db.close()
    }

    fun getAnnouncements(classroomId: String): List<Map<String, String>> {
        val announcements = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ANNOUNCEMENTS,
            arrayOf(
                COLUMN_ANNOUNCEMENT_ID,
                COLUMN_CLASSROOM_ID,
                COLUMN_UID,
                COLUMN_NAME,
                COLUMN_ANNOUNCEMENT_TEXT,
                COLUMN_TIMESTAMP
            ),
            "$COLUMN_CLASSROOM_ID = ?",
            arrayOf(classroomId),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC" // Newest first
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["announcement_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_TEXT)) ?: ""
            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
            announcements.add(map)
        }
        cursor.close()
        db.close()
        return announcements
    }


    fun insertComment(
        commentId: String,
        announcementId: String,
        classroomId: String,
        uid: String,
        name: String,
        text: String,
        timestamp: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COMMENT_ID, commentId)
            put(COLUMN_ANNOUNCEMENT_ID, announcementId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_COMMENT_TEXT, text)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        val result = db.insert(TABLE_COMMENTS, null, values)
        db.close()
        return result != -1L
    }

    fun queueCommentUpdate(
        commentId: String,
        announcementId: String,
        classroomId: String,
        uid: String,
        name: String,
        text: String,
        timestamp: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COMMENT_ID, commentId)
            put(COLUMN_ANNOUNCEMENT_ID, announcementId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_COMMENT_TEXT, text)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_COMMENT_UPDATES, null, values)
        db.close()
    }

    fun getComments(announcementId: String): List<Map<String, String>> {
        val comments = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_COMMENTS,
            arrayOf(
                COLUMN_COMMENT_ID,
                COLUMN_ANNOUNCEMENT_ID,
                COLUMN_CLASSROOM_ID,
                COLUMN_UID,
                COLUMN_NAME,
                COLUMN_COMMENT_TEXT,
                COLUMN_TIMESTAMP
            ),
            "$COLUMN_ANNOUNCEMENT_ID = ?",
            arrayOf(announcementId),
            null,
            null,
            "$COLUMN_TIMESTAMP DESC" // Newest first
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["comment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_ID)) ?: ""
            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["comment_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TEXT)) ?: ""
            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
            comments.add(map)
        }
        cursor.close()
        db.close()
        return comments
    }

    fun getUsersByClassroom(classroomId: String): List<Map<String, String>> {
        val users = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        // Query members of the classroom
        val memberCursor = db.query(
            TABLE_CLASSES,
            arrayOf(COLUMN_MEMBER_UID),
            "$COLUMN_CLASSROOM_ID = ?",
            arrayOf(classroomId),
            null, null, null
        )
        val uids = mutableListOf<String>()
        while (memberCursor.moveToNext()) {
            val uid = memberCursor.getString(memberCursor.getColumnIndexOrThrow(COLUMN_MEMBER_UID)) ?: continue
            uids.add(uid)
        }
        memberCursor.close()

        // Query user profiles for each member
        for (uid in uids) {
            val userCursor = db.query(
                TABLE_USERS,
                arrayOf(COLUMN_UID, COLUMN_NAME, COLUMN_BIO, COLUMN_PHONE, COLUMN_PROFILE_IMAGE_PATH),
                "$COLUMN_UID = ?",
                arrayOf(uid),
                null, null, null
            )
            if (userCursor.moveToFirst()) {
                val map = mutableMapOf<String, String>()
                map["uid"] = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
                map["name"] = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
                map["bio"] = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_BIO)) ?: ""
                map["phone"] = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_PHONE)) ?: ""
                map["profile_image_path"] = userCursor.getString(userCursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE_PATH)) ?: ""
                users.add(map)
            }
            userCursor.close()
        }
        db.close()
        return users
    }
}


