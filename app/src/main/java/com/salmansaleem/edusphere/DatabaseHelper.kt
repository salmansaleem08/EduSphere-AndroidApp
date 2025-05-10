package com.salmansaleem.edusphere

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EduSphere.db"
        private const val DATABASE_VERSION = 12 // Updated for submission_image_path // Updated for submissions// Updated version for classroom support
        private const val TABLE_USERS = "users"
        const val TABLE_PROFILE_UPDATES = "profile_updates"
        private const val TABLE_CLASSROOMS = "classrooms"
        const val TABLE_CLASSROOM_UPDATES = "classroom_updates"
        private const val TABLE_CLASSES = "classes"
        private const val TABLE_ANNOUNCEMENTS = "announcements"
        const val TABLE_ANNOUNCEMENT_UPDATES = "announcement_updates"
        private const val TABLE_COMMENTS = "comments"
        const val TABLE_COMMENT_UPDATES = "comment_updates"
        const val TABLE_ASSIGNMENTS = "assignments"
        const val TABLE_ASSIGNMENT_UPDATES = "assignment_updates"
        const val TABLE_SUBMISSION_UPDATES = "submission_updates"


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


        const val COLUMN_ASSIGNMENT_ID = "assignment_id"
        const val COLUMN_ASSIGNMENT_NAME = "assignment_name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_DUE_DATE = "due_date"
        const val COLUMN_SCORE = "score"
        const val COLUMN_IMAGE_PATH = "image_path"

        const val TABLE_SUBMISSIONS = "submissions"
        const val COLUMN_SUBMITTED_AT = "submitted_at"
        const val COLUMN_SUBMISSION_ID = "submission_id"

        const val COLUMN_SUBMISSION_IMAGE_PATH = "submission_image_path"


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

        val createAssignmentsTable = """
        CREATE TABLE $TABLE_ASSIGNMENTS (
            $COLUMN_ASSIGNMENT_ID TEXT PRIMARY KEY,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_ASSIGNMENT_NAME TEXT,
            $COLUMN_DESCRIPTION TEXT,
            $COLUMN_DUE_DATE TEXT,
            $COLUMN_SCORE INTEGER,
            $COLUMN_IMAGE_PATH TEXT
        )
    """.trimIndent()
        db.execSQL(createAssignmentsTable)

        val createAssignmentUpdatesTable = """
        CREATE TABLE $TABLE_ASSIGNMENT_UPDATES (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_ASSIGNMENT_ID TEXT,
            $COLUMN_CLASSROOM_ID TEXT,
            $COLUMN_UID TEXT,
            $COLUMN_ASSIGNMENT_NAME TEXT,
            $COLUMN_DESCRIPTION TEXT,
            $COLUMN_DUE_DATE TEXT,
            $COLUMN_SCORE INTEGER,
            $COLUMN_IMAGE_PATH TEXT
        )
    """.trimIndent()
        db.execSQL(createAssignmentUpdatesTable)

        val createSubmissionsTable = """
            CREATE TABLE $TABLE_SUBMISSIONS (
                $COLUMN_SUBMISSION_ID TEXT PRIMARY KEY,
                $COLUMN_ASSIGNMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_SUBMITTED_AT TEXT,
                $COLUMN_SUBMISSION_IMAGE_PATH TEXT
            )
        """.trimIndent()
        db.execSQL(createSubmissionsTable)


        val createSubmissionUpdatesTable = """
            CREATE TABLE $TABLE_SUBMISSION_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SUBMISSION_ID TEXT,
                $COLUMN_ASSIGNMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_SUBMITTED_AT TEXT,
                $COLUMN_SUBMISSION_IMAGE_PATH TEXT
            )
        """.trimIndent()
        db.execSQL(createSubmissionUpdatesTable)
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
        if (oldVersion < 9) {
            db.execSQL("""
            CREATE TABLE $TABLE_ASSIGNMENTS (
                $COLUMN_ASSIGNMENT_ID TEXT PRIMARY KEY,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_ASSIGNMENT_NAME TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_DUE_DATE TEXT,
                $COLUMN_SCORE INTEGER,
                $COLUMN_IMAGE_PATH TEXT
            )
        """.trimIndent())
            db.execSQL("""
            CREATE TABLE $TABLE_ASSIGNMENT_UPDATES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ASSIGNMENT_ID TEXT,
                $COLUMN_CLASSROOM_ID TEXT,
                $COLUMN_UID TEXT,
                $COLUMN_ASSIGNMENT_NAME TEXT,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_DUE_DATE TEXT,
                $COLUMN_SCORE INTEGER,
                $COLUMN_IMAGE_PATH TEXT
            )
        """.trimIndent())
        }
        if (oldVersion < 10) {
            db.execSQL("""
                CREATE TABLE $TABLE_SUBMISSIONS (
                    $COLUMN_SUBMISSION_ID TEXT PRIMARY KEY,
                    $COLUMN_ASSIGNMENT_ID TEXT,
                    $COLUMN_CLASSROOM_ID TEXT,
                    $COLUMN_UID TEXT,
                    $COLUMN_SUBMITTED_AT TEXT
                )
            """.trimIndent())
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE $TABLE_SUBMISSIONS ADD COLUMN $COLUMN_SUBMISSION_IMAGE_PATH TEXT")
        }
        if (oldVersion < 12) {
            db.execSQL("""
                CREATE TABLE $TABLE_SUBMISSION_UPDATES (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_SUBMISSION_ID TEXT,
                    $COLUMN_ASSIGNMENT_ID TEXT,
                    $COLUMN_CLASSROOM_ID TEXT,
                    $COLUMN_UID TEXT,
                    $COLUMN_SUBMITTED_AT TEXT,
                    $COLUMN_SUBMISSION_IMAGE_PATH TEXT
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
//    fun getClassroom(classroomId: String): Map<String, String>? {
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_CLASSROOMS,
//            arrayOf(
//                COLUMN_NAME,
//                COLUMN_SECTION,
//                COLUMN_ROOM,
//                COLUMN_SUBJECT,
//                COLUMN_CLASS_CODE,
//                COLUMN_CLASSROOM_IMAGE_PATH,
//                COLUMN_INSTRUCTOR_NAME
//            ),
//            "$COLUMN_CLASSROOM_ID = ?",
//            arrayOf(classroomId),
//            null,
//            null,
//            null
//        )
//        return if (cursor.moveToFirst()) {
//            val map = mutableMapOf<String, String>()
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
//            map["section"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
//            map["room"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
//            map["subject"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
//            map["class_code"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
//            val imagePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
//            map["image_path"] = if (imagePath.isNotEmpty()) imagePath else ""
//            map["instructor_name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
//            cursor.close()
//            map
//        } else {
//            cursor.close()
//            null
//        }
//    }

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

//    fun getUserClassrooms(uid: String): List<String> {
//        val classroomIds = mutableListOf<String>()
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_CLASSES,
//            arrayOf(COLUMN_CLASSROOM_ID),
//            "$COLUMN_MEMBER_UID = ?",
//            arrayOf(uid),
//            null,
//            null,
//            null
//        )
//        while (cursor.moveToNext()) {
//            val classroomId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: continue
//            classroomIds.add(classroomId)
//        }
//        cursor.close()
//        db.close()
//        return classroomIds
//    }

    fun getUserClassrooms(uid: String): List<String> {
        val classroomIds = mutableSetOf<String>()  // Use a set to avoid duplicates
        val db = readableDatabase

        // Fetch classrooms where the user is a member
        val memberCursor = db.query(
            TABLE_CLASSES,
            arrayOf(COLUMN_CLASSROOM_ID),
            "$COLUMN_MEMBER_UID = ?",
            arrayOf(uid),
            null,
            null,
            null
        )
        while (memberCursor.moveToNext()) {
            val classroomId = memberCursor.getString(memberCursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: continue
            classroomIds.add(classroomId)
        }
        memberCursor.close()

        // Fetch classrooms where the user is the teacher
        val teacherCursor = db.query(
            TABLE_CLASSROOMS,
            arrayOf(COLUMN_CLASSROOM_ID),
            "$COLUMN_UID = ?",  // Compare with instructor's UID
            arrayOf(uid),
            null,
            null,
            null
        )
        while (teacherCursor.moveToNext()) {
            val classroomId = teacherCursor.getString(teacherCursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: continue
            classroomIds.add(classroomId)
        }
        teacherCursor.close()

        db.close()
        return classroomIds.toList()  // Convert set to list
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

//    fun getAnnouncements(classroomId: String): List<Map<String, String>> {
//        val announcements = mutableListOf<Map<String, String>>()
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_ANNOUNCEMENTS,
//            arrayOf(
//                COLUMN_ANNOUNCEMENT_ID,
//                COLUMN_CLASSROOM_ID,
//                COLUMN_UID,
//                COLUMN_NAME,
//                COLUMN_ANNOUNCEMENT_TEXT,
//                COLUMN_TIMESTAMP
//            ),
//            "$COLUMN_CLASSROOM_ID = ?",
//            arrayOf(classroomId),
//            null,
//            null,
//            "$COLUMN_TIMESTAMP DESC" // Newest first
//        )
//        while (cursor.moveToNext()) {
//            val map = mutableMapOf<String, String>()
//            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
//            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
//            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
//            map["announcement_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_TEXT)) ?: ""
//            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
//            announcements.add(map)
//        }
//        cursor.close()
//        db.close()
//        return announcements
//    }


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
            "$COLUMN_TIMESTAMP DESC"
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            val uid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = uid
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["announcement_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_TEXT)) ?: ""
            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
            map["profile_image_path"] = getUserProfile(uid)?.get("profile_image_path") ?: ""
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

//    fun getComments(announcementId: String): List<Map<String, String>> {
//        val comments = mutableListOf<Map<String, String>>()
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_COMMENTS,
//            arrayOf(
//                COLUMN_COMMENT_ID,
//                COLUMN_ANNOUNCEMENT_ID,
//                COLUMN_CLASSROOM_ID,
//                COLUMN_UID,
//                COLUMN_NAME,
//                COLUMN_COMMENT_TEXT,
//                COLUMN_TIMESTAMP
//            ),
//            "$COLUMN_ANNOUNCEMENT_ID = ?",
//            arrayOf(announcementId),
//            null,
//            null,
//            "$COLUMN_TIMESTAMP DESC" // Newest first
//        )
//        while (cursor.moveToNext()) {
//            val map = mutableMapOf<String, String>()
//            map["comment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_ID)) ?: ""
//            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
//            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
//            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
//            map["comment_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TEXT)) ?: ""
//            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
//            comments.add(map)
//        }
//        cursor.close()
//        db.close()
//        return comments
//    }




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
            "$COLUMN_TIMESTAMP DESC"
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            val uid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["comment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_ID)) ?: ""
            map["announcement_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ANNOUNCEMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = uid
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["comment_text"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT_TEXT)) ?: ""
            map["timestamp"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)) ?: ""
            map["profile_image_path"] = getUserProfile(uid)?.get("profile_image_path") ?: ""
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




    fun insertAssignment(
        assignmentId: String,
        classroomId: String,
        uid: String,
        name: String,
        description: String,
        dueDate: String,
        score: Int,
        imagePath: String?
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSIGNMENT_ID, assignmentId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_ASSIGNMENT_NAME, name)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_DUE_DATE, dueDate)
            put(COLUMN_SCORE, score)
            put(COLUMN_IMAGE_PATH, imagePath)
        }
        val result = db.insert(TABLE_ASSIGNMENTS, null, values)
        db.close()
        return result != -1L
    }

    fun queueAssignmentUpdate(
        assignmentId: String,
        classroomId: String,
        uid: String,
        name: String,
        description: String,
        dueDate: String,
        score: Int,
        imagePath: String?
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ASSIGNMENT_ID, assignmentId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_ASSIGNMENT_NAME, name)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_DUE_DATE, dueDate)
            put(COLUMN_SCORE, score)
            put(COLUMN_IMAGE_PATH, imagePath)
        }
        db.insert(TABLE_ASSIGNMENT_UPDATES, null, values)
        db.close()
    }

    fun getAssignments(classroomId: String): List<Map<String, String>> {
        val assignments = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ASSIGNMENTS,
            arrayOf(
                COLUMN_ASSIGNMENT_ID,
                COLUMN_CLASSROOM_ID,
                COLUMN_UID,
                COLUMN_ASSIGNMENT_NAME,
                COLUMN_DESCRIPTION,
                COLUMN_DUE_DATE,
                COLUMN_SCORE,
                COLUMN_IMAGE_PATH
            ),
            "$COLUMN_CLASSROOM_ID = ?",
            arrayOf(classroomId),
            null,
            null,
            "$COLUMN_DUE_DATE ASC"
        )
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
            map["description"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
            map["score"] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE)).toString()
            map["image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)) ?: ""
            assignments.add(map)
        }
        cursor.close()
        db.close()
        return assignments
    }

    fun insertSubmission(
        submissionId: String,
        assignmentId: String,
        classroomId: String,
        uid: String,
        submittedAt: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SUBMISSION_ID, submissionId)
            put(COLUMN_ASSIGNMENT_ID, assignmentId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_SUBMITTED_AT, submittedAt)
        }
        val result = db.insert(TABLE_SUBMISSIONS, null, values)
        db.close()
        return result != -1L
    }

//    fun getAssignmentsWithStatus(classroomId: String, userId: String): List<Map<String, String>> {
//        val assignments = mutableListOf<Map<String, String>>()
//        val db = readableDatabase
//        val query = """
//            SELECT a.$COLUMN_ASSIGNMENT_ID, a.$COLUMN_CLASSROOM_ID, a.$COLUMN_UID,
//                   a.$COLUMN_ASSIGNMENT_NAME, a.$COLUMN_DUE_DATE,
//                   s.$COLUMN_SUBMISSION_ID, s.$COLUMN_SUBMITTED_AT
//            FROM $TABLE_ASSIGNMENTS a
//            LEFT JOIN $TABLE_SUBMISSIONS s ON a.$COLUMN_ASSIGNMENT_ID = s.$COLUMN_ASSIGNMENT_ID
//                AND s.$COLUMN_UID = ?
//            WHERE a.$COLUMN_CLASSROOM_ID = ?
//            ORDER BY a.$COLUMN_DUE_DATE ASC
//        """.trimIndent()
//        val cursor = db.rawQuery(query, arrayOf(userId, classroomId))
//        while (cursor.moveToNext()) {
//            val map = mutableMapOf<String, String>()
//            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
//            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
//            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
//            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
//            map["submission_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_ID)) ?: ""
//            map["submitted_at"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_AT)) ?: ""
//            assignments.add(map)
//        }
//        cursor.close()
//        db.close()
//        return assignments
//    }



    fun getAssignmentsWithStatus(classroomId: String, userId: String): List<Map<String, String>> {
        val assignments = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val query = """
        SELECT a.$COLUMN_ASSIGNMENT_ID, a.$COLUMN_CLASSROOM_ID, a.$COLUMN_UID, 
               a.$COLUMN_ASSIGNMENT_NAME, a.$COLUMN_DUE_DATE, 
               s.$COLUMN_SUBMISSION_ID, s.$COLUMN_SUBMITTED_AT, u.$COLUMN_NAME AS teacher_name
        FROM $TABLE_ASSIGNMENTS a
        LEFT JOIN $TABLE_SUBMISSIONS s ON a.$COLUMN_ASSIGNMENT_ID = s.$COLUMN_ASSIGNMENT_ID 
            AND s.$COLUMN_UID = ?
        LEFT JOIN $TABLE_USERS u ON a.$COLUMN_UID = u.$COLUMN_UID
        WHERE a.$COLUMN_CLASSROOM_ID = ?
        ORDER BY a.$COLUMN_DUE_DATE ASC
    """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId, classroomId))
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
            map["submission_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_ID)) ?: ""
            map["submitted_at"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_AT)) ?: ""
            map["teacher_name"] = cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")) ?: ""
            assignments.add(map)
        }
        cursor.close()
        db.close()
        return assignments
    }

//    fun getAssignment(assignmentId: String): Map<String, String>? {
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_ASSIGNMENTS,
//            arrayOf(
//                COLUMN_ASSIGNMENT_ID,
//                COLUMN_CLASSROOM_ID,
//                COLUMN_UID,
//                COLUMN_ASSIGNMENT_NAME,
//                COLUMN_DESCRIPTION,
//                COLUMN_DUE_DATE,
//                COLUMN_SCORE,
//                COLUMN_IMAGE_PATH
//            ),
//            "$COLUMN_ASSIGNMENT_ID = ?",
//            arrayOf(assignmentId),
//            null,
//            null,
//            null
//        )
//        return if (cursor.moveToFirst()) {
//            val map = mutableMapOf<String, String>()
//            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
//            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
//            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
//            map["description"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
//            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
//            map["score"] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE)).toString()
//            map["image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)) ?: ""
//            cursor.close()
//            db.close()
//            map
//        } else {
//            cursor.close()
//            db.close()
//            null
//        }
//    }


    fun getAssignment(assignmentId: String): Map<String, String>? {
        val db = readableDatabase
        val query = """
        SELECT a.$COLUMN_ASSIGNMENT_ID, a.$COLUMN_CLASSROOM_ID, a.$COLUMN_UID, 
               a.$COLUMN_ASSIGNMENT_NAME, a.$COLUMN_DESCRIPTION, a.$COLUMN_DUE_DATE, 
               a.$COLUMN_SCORE, a.$COLUMN_IMAGE_PATH, u.$COLUMN_NAME AS teacher_name
        FROM $TABLE_ASSIGNMENTS a
        LEFT JOIN $TABLE_USERS u ON a.$COLUMN_UID = u.$COLUMN_UID
        WHERE a.$COLUMN_ASSIGNMENT_ID = ?
    """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(assignmentId))
        return if (cursor.moveToFirst()) {
            val map = mutableMapOf<String, String>()
            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
            map["description"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)) ?: ""
            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
            map["score"] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SCORE)).toString()
            map["image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)) ?: ""
            map["teacher_name"] = cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")) ?: ""
            cursor.close()
            db.close()
            map
        } else {
            cursor.close()
            db.close()
            null
        }
    }


    fun insertSubmission(
        submissionId: String,
        assignmentId: String,
        classroomId: String,
        uid: String,
        submittedAt: String,
        submissionImagePath: String?
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SUBMISSION_ID, submissionId)
            put(COLUMN_ASSIGNMENT_ID, assignmentId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_SUBMITTED_AT, submittedAt)
            put(COLUMN_SUBMISSION_IMAGE_PATH, submissionImagePath)
        }
        val result = db.insert(TABLE_SUBMISSIONS, null, values)
        db.close()
        return result != -1L
    }

    fun getSubmission(assignmentId: String, userId: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SUBMISSIONS,
            arrayOf(
                COLUMN_SUBMISSION_ID,
                COLUMN_ASSIGNMENT_ID,
                COLUMN_CLASSROOM_ID,
                COLUMN_UID,
                COLUMN_SUBMITTED_AT,
                COLUMN_SUBMISSION_IMAGE_PATH
            ),
            "$COLUMN_ASSIGNMENT_ID = ? AND $COLUMN_UID = ?",
            arrayOf(assignmentId, userId),
            null,
            null,
            null
        )
        return if (cursor.moveToFirst()) {
            val map = mutableMapOf<String, String>()
            map["submission_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_ID)) ?: ""
            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["submitted_at"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_AT)) ?: ""
            map["submission_image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_IMAGE_PATH)) ?: ""
            cursor.close()
            db.close()
            map
        } else {
            cursor.close()
            db.close()
            null
        }
    }



    fun queueSubmissionUpdate(
        submissionId: String,
        assignmentId: String,
        classroomId: String,
        uid: String,
        submittedAt: String,
        submissionImagePath: String?
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SUBMISSION_ID, submissionId)
            put(COLUMN_ASSIGNMENT_ID, assignmentId)
            put(COLUMN_CLASSROOM_ID, classroomId)
            put(COLUMN_UID, uid)
            put(COLUMN_SUBMITTED_AT, submittedAt)
            put(COLUMN_SUBMISSION_IMAGE_PATH, submissionImagePath)
        }
        db.insert(TABLE_SUBMISSION_UPDATES, null, values)
        db.close()
    }

    fun getTeacherNameByUid(uid: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_NAME),
            "$COLUMN_UID = ?",
            arrayOf(uid),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            cursor.close()
            db.close()
            name
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun getUserUidByName(name: String): String? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_UID),
            "$COLUMN_NAME = ?",
            arrayOf(name),
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val uid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID))
            cursor.close()
            uid
        } else {
            cursor.close()
            null
        }
    }



//    fun getClassroom(classroomId: String): Map<String, String>? {
//        val db = readableDatabase
//        val cursor = db.query(
//            TABLE_CLASSROOMS,
//            arrayOf(
//                COLUMN_CLASSROOM_ID,
//                COLUMN_UID,
//                COLUMN_NAME,
//                COLUMN_SECTION,
//                COLUMN_ROOM,
//                COLUMN_SUBJECT,
//                COLUMN_CLASS_CODE,
//                COLUMN_CLASSROOM_IMAGE_PATH,
//                COLUMN_INSTRUCTOR_NAME
//            ),
//            "$COLUMN_CLASSROOM_ID = ?",
//            arrayOf(classroomId),
//            null, null, null
//        )
//        return if (cursor.moveToFirst()) {
//            val map = mutableMapOf<String, String>()
//            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
//            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
//            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
//            map["section"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
//            map["room"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
//            map["subject"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
//            map["class_code"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
//            map["classroom_image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
//            map["instructor_name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
//            cursor.close()
//            map
//        } else {
//            cursor.close()
//            null
//        }
//    }


    fun getClassroom(classroomId: String): Map<String, String>? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_CLASSROOMS,
            arrayOf(
                COLUMN_CLASSROOM_ID,
                COLUMN_UID,
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
            null, null, null
        )
        return if (cursor.moveToFirst()) {
            val map = mutableMapOf<String, String>()
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)) ?: ""
            map["section"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SECTION)) ?: ""
            map["room"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROOM)) ?: ""
            map["subject"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBJECT)) ?: ""
            map["class_code"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_CODE)) ?: ""
            map["image_path"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_IMAGE_PATH)) ?: ""
            map["instructor_name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTOR_NAME)) ?: ""
            cursor.close()
            map
        } else {
            cursor.close()
            null
        }
    }

    fun deleteSubmissionUpdate(submissionId: String): Boolean {
        val db = writableDatabase
        val rowsAffected = db.delete(
            TABLE_SUBMISSION_UPDATES,
            "$COLUMN_SUBMISSION_ID = ?",
            arrayOf(submissionId)
        )
        db.close()
        return rowsAffected > 0
    }


    fun insertOrUpdateUser(uid: String, name: String, email: String, phone: String, bio: String?): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_UID, uid)
            put(COLUMN_NAME, name)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PHONE, phone)
            put(COLUMN_BIO, bio)
        }
        // Insert or replace if UID already exists
        val result = db.insertWithOnConflict(
            TABLE_USERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        return result != -1L
    }

    fun getAllAssignmentsWithStatus(userId: String, classroomIds: List<String>): List<Map<String, String>> {
        if (classroomIds.isEmpty()) return emptyList()
        val db = readableDatabase
        val placeholders = classroomIds.joinToString(",") { "?" }
        val query = """
        SELECT a.$COLUMN_ASSIGNMENT_ID, a.$COLUMN_CLASSROOM_ID, a.$COLUMN_UID, 
               a.$COLUMN_ASSIGNMENT_NAME, a.$COLUMN_DUE_DATE, 
               s.$COLUMN_SUBMISSION_ID, s.$COLUMN_SUBMITTED_AT, u.$COLUMN_NAME AS teacher_name
        FROM $TABLE_ASSIGNMENTS a
        INNER JOIN $TABLE_CLASSES c ON a.$COLUMN_CLASSROOM_ID = c.$COLUMN_CLASSROOM_ID 
            AND c.$COLUMN_MEMBER_UID = ?
        LEFT JOIN $TABLE_SUBMISSIONS s ON a.$COLUMN_ASSIGNMENT_ID = s.$COLUMN_ASSIGNMENT_ID 
            AND s.$COLUMN_UID = ?
        LEFT JOIN $TABLE_USERS u ON a.$COLUMN_UID = u.$COLUMN_UID
        WHERE a.$COLUMN_CLASSROOM_ID IN ($placeholders)
        ORDER BY a.$COLUMN_DUE_DATE ASC
    """.trimIndent()
        val args = arrayOf(userId, userId) + classroomIds.toTypedArray()
        val cursor = db.rawQuery(query, args)
        val assignments = mutableListOf<Map<String, String>>()
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["assignment_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_ID)) ?: ""
            map["classroom_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASSROOM_ID)) ?: ""
            map["uid"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UID)) ?: ""
            map["name"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ASSIGNMENT_NAME)) ?: ""
            map["due_date"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE)) ?: ""
            map["submission_id"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMISSION_ID)) ?: ""
            map["submitted_at"] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUBMITTED_AT)) ?: ""
            map["teacher_name"] = cursor.getString(cursor.getColumnIndexOrThrow("teacher_name")) ?: ""
            assignments.add(map)
        }
        cursor.close()
        db.close()
        return assignments
    }
}


