package com.salmansaleem.edusphere

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import com.google.firebase.database.ktx.getValue
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec


class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val TAG = "SyncWorker"
    private val databaseHelper = DatabaseHelper(appContext)
    private val database = FirebaseDatabase.getInstance()

    private val client = OkHttpClient()


    private val apiService: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(IP.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            var allSyncedSuccessfully = true

            // Sync profile updates (existing)
            val profileDb = databaseHelper.readableDatabase
            val profileCursor = profileDb.query(
                DatabaseHelper.TABLE_PROFILE_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_NAME,
                    DatabaseHelper.COLUMN_BIO,
                    DatabaseHelper.COLUMN_PHONE,
                    DatabaseHelper.COLUMN_PROFILE_IMAGE_PATH
                ),
                null, null, null, null, null
            )

            while (profileCursor.moveToNext()) {
                val id = profileCursor.getLong(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val uid = profileCursor.getString(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = profileCursor.getString(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)) ?: ""
                val bio = profileCursor.getString(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BIO)) ?: ""
                val phone = profileCursor.getString(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)) ?: ""
                val profileImagePath = profileCursor.getString(profileCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROFILE_IMAGE_PATH))

                val userData = mapOf(
                    "fullName" to name,
                    "bio" to bio,
                    "phone" to phone
                )
                var firebaseSuccess = false
                try {
                    database.getReference("Users").child(uid).updateChildren(userData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for uid $uid")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for uid $uid: ${e.message}")
                    allSyncedSuccessfully = false
                }

                var imageSuccess = true
                if (profileImagePath != null) {
                    val file = File(profileImagePath)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()

                            val imagePart = MultipartBody.Part.createFormData(
                                "image",
                                "${uid}_PROFILE_IMAGE.png",
                                RequestBody.create("image/png".toMediaType(), byteArray)
                            )
                            val uidPart = RequestBody.create("text/plain".toMediaType(), uid)

                            val response = apiService.uploadProfileImage(uidPart, imagePart).execute()
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.d(TAG, "Synced image for uid $uid: ${response.body()?.image_url}")
                                response.body()?.image_url?.let { url ->
                                    try {
                                        val bitmapFromServer = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                                        val fileName = "${uid}_profile.png"
                                        val newFile = File(applicationContext.filesDir, fileName)
                                        FileOutputStream(newFile).use { out ->
                                            bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        Log.d(TAG, "Saved synced image locally at: ${newFile.absolutePath}")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving synced image locally: ${e.message}")
                                        allSyncedSuccessfully = false
                                        imageSuccess = false
                                    }
                                }
                            } else {
                                Log.e(TAG, "Image sync failed for uid $uid: ${response.code()} ${response.message()}")
                                allSyncedSuccessfully = false
                                imageSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing image for uid $uid: ${e.message}")
                            allSyncedSuccessfully = false
                            imageSuccess = false
                        }
                    } else {
                        Log.w(TAG, "Image file does not exist at path: $profileImagePath")
                        allSyncedSuccessfully = false
                        imageSuccess = false
                    }
                }

                if (firebaseSuccess && imageSuccess) {
                    try {
                        profileDb.delete(
                            DatabaseHelper.TABLE_PROFILE_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued profile update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued profile update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            profileCursor.close()
            profileDb.close()

            // Sync classroom updates
            // Sync classroom updates
            val classroomDb = databaseHelper.readableDatabase
            val classroomCursor = classroomDb.query(
                DatabaseHelper.TABLE_CLASSROOM_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_CLASSROOM_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_NAME,
                    DatabaseHelper.COLUMN_SECTION,
                    DatabaseHelper.COLUMN_ROOM,
                    DatabaseHelper.COLUMN_SUBJECT,
                    DatabaseHelper.COLUMN_CLASS_CODE,
                    DatabaseHelper.COLUMN_CLASSROOM_IMAGE_PATH,
                    DatabaseHelper.COLUMN_INSTRUCTOR_NAME
                ),
                null, null, null, null, null
            )

            while (classroomCursor.moveToNext()) {
                val id = classroomCursor.getLong(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val classroomId = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_ID))
                val uid = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)) ?: ""
                val section = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SECTION)) ?: ""
                val room = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ROOM)) ?: ""
                val subject = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUBJECT)) ?: ""
                val classCode = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASS_CODE)) ?: ""
                val classroomImagePath = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_IMAGE_PATH))
                val instructorName = classroomCursor.getString(classroomCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_INSTRUCTOR_NAME)) ?: ""
                val classroomData = mapOf(
                    "name" to name,
                    "section" to section,
                    "room" to room,
                    "subject" to subject,
                    "class_code" to classCode,
                    "uid" to uid,
                    "instructor_name" to instructorName
                )
                var firebaseSuccess = false
                try {
                    database.getReference("Classrooms").child(classroomId).setValue(classroomData).await()
                    // Add creator as member in Firebase Classes table
                    database.getReference("Classes").child(classroomId).child("members").child(uid).setValue(true).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for classroom $classroomId")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for classroom $classroomId: ${e.message}")
                    allSyncedSuccessfully = false
                }
                var imageSuccess = true
                if (classroomImagePath != null) {
                    val file = File(classroomImagePath)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()

                            val imagePart = MultipartBody.Part.createFormData(
                                "image",
                                "${classroomId}_CLASSROOM_IMAGE.png",
                                RequestBody.create("image/png".toMediaType(), byteArray)
                            )
                            val classroomIdPart = RequestBody.create("text/plain".toMediaType(), classroomId)

                            val response = apiService.uploadClassroomImage(classroomIdPart, imagePart).execute()
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.d(TAG, "Synced classroom image for classroom $classroomId: ${response.body()?.image_url}")
                                response.body()?.image_url?.let { url ->
                                    try {
                                        val bitmapFromServer = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                                        val fileName = "${classroomId}_classroom.png"
                                        val newFile = File(applicationContext.filesDir, fileName)
                                        FileOutputStream(newFile).use { out ->
                                            bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        Log.d(TAG, "Saved synced classroom image locally at: ${newFile.absolutePath}")

                                        // Update SQLite with local path
                                        databaseHelper.insertClassroom(
                                            classroomId, uid, name, section, room, subject, classCode, newFile.absolutePath, instructorName
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving synced classroom image locally: ${e.message}")
                                        allSyncedSuccessfully = false
                                        imageSuccess = false
                                    }
                                }
                            } else {
                                Log.e(TAG, "Classroom image sync failed for classroom $classroomId: ${response.code()} ${response.message()}")
                                allSyncedSuccessfully = false
                                imageSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing classroom image for classroom $classroomId: ${e.message}")
                            allSyncedSuccessfully = false
                            imageSuccess = false
                        }
                    } else {
                        Log.w(TAG, "Classroom image file does not exist at path: $classroomImagePath")
                        allSyncedSuccessfully = false
                        imageSuccess = false
                    }
                }

                if (firebaseSuccess && imageSuccess) {
                    try {
                        classroomDb.delete(
                            DatabaseHelper.TABLE_CLASSROOM_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued classroom update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued classroom update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            classroomCursor.close()
            classroomDb.close()



            val announcementDb = databaseHelper.readableDatabase
            val announcementCursor = announcementDb.query(
                DatabaseHelper.TABLE_ANNOUNCEMENT_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_ANNOUNCEMENT_ID,
                    DatabaseHelper.COLUMN_CLASSROOM_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_NAME,
                    DatabaseHelper.COLUMN_ANNOUNCEMENT_TEXT,
                    DatabaseHelper.COLUMN_TIMESTAMP
                ),
                null, null, null, null, null
            )
            while (announcementCursor.moveToNext()) {
                val id = announcementCursor.getLong(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val announcementId = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ANNOUNCEMENT_ID))
                val classroomId = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_ID))
                val uid = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)) ?: ""
                val text = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ANNOUNCEMENT_TEXT)) ?: ""
                val timestamp = announcementCursor.getString(announcementCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)) ?: ""
                val announcementData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "text" to text,
                    "timestamp" to timestamp
                )
                var firebaseSuccess = false
                try {
                    database.getReference("Announcements").child(classroomId).child(announcementId).setValue(announcementData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for announcement $announcementId")
                    val classroomSnapshot = database.getReference("Classrooms").child(classroomId).get().await()
                    val className = classroomSnapshot.child("name").getValue(String::class.java) ?: "Classroom"
                    val announcerName = name
                    sendAnnouncementNotification(classroomId, announcementId, announcerName, className, uid, text) // Added line
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for announcement $announcementId: ${e.message}")
                    allSyncedSuccessfully = false
                }
                if (firebaseSuccess) {
                    try {
                        announcementDb.delete(
                            DatabaseHelper.TABLE_ANNOUNCEMENT_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued announcement update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued announcement update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            announcementCursor.close()
            announcementDb.close()


            val commentDb = databaseHelper.readableDatabase
            val commentCursor = commentDb.query(
                DatabaseHelper.TABLE_COMMENT_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_COMMENT_ID,
                    DatabaseHelper.COLUMN_ANNOUNCEMENT_ID,
                    DatabaseHelper.COLUMN_CLASSROOM_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_NAME,
                    DatabaseHelper.COLUMN_COMMENT_TEXT,
                    DatabaseHelper.COLUMN_TIMESTAMP
                ),
                null, null, null, null, null
            )

            while (commentCursor.moveToNext()) {
                val id = commentCursor.getLong(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val commentId = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMMENT_ID))
                val announcementId = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ANNOUNCEMENT_ID))
                val classroomId = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_ID))
                val uid = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)) ?: ""
                val text = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMMENT_TEXT)) ?: ""
                val timestamp = commentCursor.getString(commentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIMESTAMP)) ?: ""
                val commentData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "text" to text,
                    "timestamp" to timestamp
                )
                var firebaseSuccess = false
                try {
                    database.getReference("Announcements").child(classroomId).child(announcementId).child("comments").child(commentId).setValue(commentData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for comment $commentId")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for comment $commentId: ${e.message}")
                    allSyncedSuccessfully = false
                }
                if (firebaseSuccess) {
                    try {
                        commentDb.delete(
                            DatabaseHelper.TABLE_COMMENT_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued comment update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued comment update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            commentCursor.close()
            commentDb.close()




            val assignmentDb = databaseHelper.readableDatabase
            val assignmentCursor = assignmentDb.query(
                DatabaseHelper.TABLE_ASSIGNMENT_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_ASSIGNMENT_ID,
                    DatabaseHelper.COLUMN_CLASSROOM_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_ASSIGNMENT_NAME,
                    DatabaseHelper.COLUMN_DESCRIPTION,
                    DatabaseHelper.COLUMN_DUE_DATE,
                    DatabaseHelper.COLUMN_SCORE,
                    DatabaseHelper.COLUMN_IMAGE_PATH
                ),
                null, null, null, null, null
            )
            while (assignmentCursor.moveToNext()) {
                val id = assignmentCursor.getLong(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val assignmentId = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ASSIGNMENT_ID))
                val classroomId = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_ID))
                val uid = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ASSIGNMENT_NAME)) ?: ""
                val description = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION)) ?: ""
                val dueDate = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DUE_DATE)) ?: ""
                val score = assignmentCursor.getInt(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SCORE))
                val imagePath = assignmentCursor.getString(assignmentCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IMAGE_PATH))
                val assignmentData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "description" to description,
                    "due_date" to dueDate,
                    "score" to score,
                    "timestamp" to java.text.SimpleDateFormat("hh:mm a · dd MMM yy", java.util.Locale.getDefault()).format(java.util.Date())
                )
                var firebaseSuccess = false
                try {
                    database.getReference("Assignments").child(classroomId).child(assignmentId).setValue(assignmentData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for assignment $assignmentId")
                    databaseHelper.getUserProfile(uid)?.let { user ->
                        val announcerName = user["name"] ?: "User"
                        sendAssignmentNotification(classroomId, assignmentId, announcerName, name, uid)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for assignment $assignmentId: ${e.message}")
                    allSyncedSuccessfully = false
                }
                var imageSuccess = true
                if (imagePath != null) {
                    val file = File(imagePath)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            val imagePart = MultipartBody.Part.createFormData(
                                "image",
                                "${assignmentId}_ASSIGNMENT_IMAGE.png",
                                RequestBody.create("image/png".toMediaType(), byteArray)
                            )
                            val assignmentIdPart = RequestBody.create("text/plain".toMediaType(), assignmentId)
                            val response = apiService.uploadAssignmentImage(assignmentIdPart, imagePart).execute()
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.d(TAG, "Synced assignment image for assignment $assignmentId: ${response.body()?.image_url}")
                                response.body()?.image_url?.let { url ->
                                    try {
                                        val bitmapFromServer = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                                        val fileName = "${assignmentId}_assignment.png"
                                        val newFile = File(applicationContext.filesDir, fileName)
                                        FileOutputStream(newFile).use { out ->
                                            bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        Log.d(TAG, "Saved synced assignment image locally at: ${newFile.absolutePath}")
                                        databaseHelper.insertAssignment(
                                            assignmentId, classroomId, uid, name, description, dueDate, score, newFile.absolutePath
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving synced assignment image locally: ${e.message}")
                                        allSyncedSuccessfully = false
                                        imageSuccess = false
                                    }
                                }
                            } else {
                                Log.e(TAG, "Assignment image sync failed for assignment $assignmentId: ${response.code()} ${response.message()}")
                                allSyncedSuccessfully = false
                                imageSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing assignment image for assignment $assignmentId: ${e.message}")
                            allSyncedSuccessfully = false
                            imageSuccess = false
                        }
                    } else {
                        Log.w(TAG, "Assignment image file does not exist at path: $imagePath")
                        allSyncedSuccessfully = false
                        imageSuccess = false
                    }
                }
                if (firebaseSuccess && imageSuccess) {
                    try {
                        assignmentDb.delete(
                            DatabaseHelper.TABLE_ASSIGNMENT_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued assignment update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued assignment update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            assignmentCursor.close()
            assignmentDb.close()



            val submissionDb = databaseHelper.readableDatabase
            val submissionCursor = submissionDb.query(
                DatabaseHelper.TABLE_SUBMISSION_UPDATES,
                arrayOf(
                    DatabaseHelper.COLUMN_ID,
                    DatabaseHelper.COLUMN_SUBMISSION_ID,
                    DatabaseHelper.COLUMN_ASSIGNMENT_ID,
                    DatabaseHelper.COLUMN_CLASSROOM_ID,
                    DatabaseHelper.COLUMN_UID,
                    DatabaseHelper.COLUMN_SUBMITTED_AT,
                    DatabaseHelper.COLUMN_SUBMISSION_IMAGE_PATH
                ),
                null, null, null, null, null
            )

            while (submissionCursor.moveToNext()) {
                val id = submissionCursor.getLong(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val submissionId = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUBMISSION_ID))
                val assignmentId = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ASSIGNMENT_ID))
                val classroomId = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CLASSROOM_ID))
                val uid = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val submittedAt = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUBMITTED_AT)) ?: ""
                val submissionImagePath = submissionCursor.getString(submissionCursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_SUBMISSION_IMAGE_PATH))

                val submissionData = mapOf(
                    "uid" to uid,
                    "assignment_id" to assignmentId,
                    "classroom_id" to classroomId,
                    "submitted_at" to submittedAt
                )

                var firebaseSuccess = false
                try {
                    database.getReference("Submissions").child(classroomId).child(uid).child(assignmentId).setValue(submissionData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for submission $submissionId")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for submission $submissionId: ${e.message}")
                    allSyncedSuccessfully = false
                }

                var imageSuccess = true
                if (submissionImagePath != null) {
                    val file = File(submissionImagePath)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()

                            val imagePart = MultipartBody.Part.createFormData(
                                "image",
                                "${submissionId}_SUBMISSION_IMAGE.png",
                                RequestBody.create("image/png".toMediaType(), byteArray)
                            )
                            val submissionIdPart = RequestBody.create("text/plain".toMediaType(), submissionId)

                            val response = apiService.uploadSubmissionImage(submissionIdPart, imagePart).execute()
                            if (response.isSuccessful && response.body()?.success == true) {
                                Log.d(TAG, "Synced submission image for submission $submissionId: ${response.body()?.image_url}")
                                response.body()?.image_url?.let { url ->
                                    try {
                                        val bitmapFromServer = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                                        val fileName = "${submissionId}_submission.png"
                                        val newFile = File(applicationContext.filesDir, fileName)
                                        FileOutputStream(newFile).use { out ->
                                            bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        Log.d(TAG, "Saved synced submission image locally at: ${newFile.absolutePath}")
                                        databaseHelper.insertSubmission(
                                            submissionId, assignmentId, classroomId, uid, submittedAt, newFile.absolutePath
                                        )
                                        databaseHelper.deleteSubmissionUpdate(submissionId)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error saving synced submission image locally: ${e.message}")
                                        allSyncedSuccessfully = false
                                        imageSuccess = false
                                    }
                                }
                            } else {
                                Log.e(TAG, "Submission image sync failed for submission $submissionId: ${response.code()} ${response.body()?.error ?: response.message()}")
                                allSyncedSuccessfully = false
                                imageSuccess = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing submission image for submission $submissionId: ${e.message}")
                            allSyncedSuccessfully = false
                            imageSuccess = false
                        }
                    } else {
                        Log.w(TAG, "Submission image file does not exist at path: $submissionImagePath")
                        allSyncedSuccessfully = false
                        imageSuccess = false
                    }
                }

                if (firebaseSuccess && imageSuccess) {
                    try {
                        submissionDb.delete(
                            DatabaseHelper.TABLE_SUBMISSION_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued submission update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued submission update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            submissionCursor.close()
            submissionDb.close()




            if (allSyncedSuccessfully) {
                Log.d(TAG, "All updates synced successfully")
                Result.success()
            } else {
                Log.w(TAG, "Some updates failed to sync, scheduling retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            Result.retry()
        }
    }



    private suspend fun sendAnnouncementNotification(
        classroomId: String,
        announcementId: String,
        announcerName: String,
        className: String,
        senderUid: String, // Add sender's UID to exclude them
        announcementText: String // Add announcement text for notification body
    ) {
        val membersRef = FirebaseDatabase.getInstance().getReference("Classes").child(classroomId).child("members")
        val snapshot = membersRef.get().await()
        val memberUids = snapshot.children.mapNotNull { it.key }.filter { it != senderUid } // Exclude sender
        Log.d("FCM", "Members for classroom $classroomId (excluding sender $senderUid): $memberUids")
        for (uid in memberUids) {
            val tokenSnapshot = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken").get().await()
            val token = tokenSnapshot.getValue(String::class.java)
            Log.d("FCM", "Token for UID $uid: $token")
            if (token != null) {
                val title = "$announcerName announced in $className"
                val body = if (announcementText.length > 100) "${announcementText.take(100)}..." else announcementText
                sendNotificationToToken(token, title, body, classroomId, announcementId, uid)
            }
        }
    }


    private fun sendNotificationToToken(
        token: String,
        title: String,
        body: String,
        classroomId: String,
        announcementId: String,
        uid: String
    ) {
        Thread {
            try {
                val accessToken = getAccessToken()
                if (accessToken.isEmpty()) {
                    Log.e("FCM", "Failed to obtain access token")
                    return@Thread
                }
                val projectId = "edusphere-e2107" // Replace with your Firebase project ID
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
                val json = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", token)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("data", JSONObject().apply {
                            put("type", "announcement")
                            put("classroomId", classroomId)
                            put("announcementId", announcementId)
                        })
                        put("android", JSONObject().apply {
                            put("priority", "high")
                        })
                    })
                }.toString()
                Log.d("FCM", "Sending JSON to $token: $json")

                val request = Request.Builder()
                    .url(url)
                    .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("FCM", "Notification sent successfully to $token")
                    } else {
                        val errorBody = response.body?.string()
                        Log.e("FCM", "FCM v1 failed: ${response.code} - $errorBody")
                        if (response.code == 404) {
                            Log.e("FCM", "Invalid token $token, removing for UID $uid")
                            FirebaseDatabase.getInstance().reference
                                .child("Users")
                                .child(uid)
                                .child("fcmToken")
                                .removeValue()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM", "Error sending FCM v1: ${e.message}", e)
            }
        }.start()
    }


    private fun getAccessToken(): String {
        try {
            val inputStream: InputStream = applicationContext.resources.openRawResource(R.raw.service_account)
            //val inputStream: InputStream = resources.openRawResource(R.raw.service_account)
            // val inputStream: InputStream = resources.openRawResource(com.google.firebase.database.R.raw.service_account)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)
            val clientEmail = json.getString("client_email")
            val privateKeyPem = json.getString("private_key")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .trim()
            val tokenUri = json.getString("token_uri")

            Log.d("FCM", "Raw private key (first 50 chars): ${privateKeyPem.take(50)}...")

            // Decode using android.util.Base64
            val keyBytes = try {
                Base64.decode(privateKeyPem, Base64.DEFAULT) // Use DEFAULT flag
            } catch (e: IllegalArgumentException) {
                Log.e("FCM", "Base64 decode failed: ${e.message}", e)
                return ""
            }
            Log.d("FCM", "Decoded key length: ${keyBytes.size} bytes")

            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = try {
                keyFactory.generatePrivate(keySpec)
            } catch (e: Exception) {
                Log.e("FCM", "Private key parsing failed: ${e.message}", e)
                return ""
            }

            val now = System.currentTimeMillis() / 1000
            val jwt = Jwts.builder()
                .setHeaderParam("alg", "RS256")
                .setHeaderParam("typ", "JWT")
                .setIssuer(clientEmail)
                .setAudience(tokenUri)
                .setIssuedAt(java.util.Date(now * 1000))
                .setExpiration(java.util.Date((now + 3600) * 1000))
                .claim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact()
            Log.d("FCM", "Generated JWT: $jwt")

            val requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url(tokenUri)
                .post(requestBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val responseJson = JSONObject(responseBody)
                    val accessToken = responseJson.getString("access_token")
                    Log.d("FCM", "Access token obtained: $accessToken")
                    return accessToken
                } else {
                    Log.e("FCM", "Token request failed: ${response.code} - $responseBody")
                    return ""
                }
            }
        } catch (e: Exception) {
            Log.e("FCM", "Error getting access token: ${e.message}", e)
            return ""
        }
    }



    private suspend fun sendAssignmentNotification(
        classroomId: String,
        assignmentId: String,
        announcerName: String,
        assignmentName: String,
        senderUid: String
    ) {
        val membersRef = FirebaseDatabase.getInstance().getReference("Classes").child(classroomId).child("members")
        val snapshot = membersRef.get().await()
        val memberUids = snapshot.children.mapNotNull { it.key }.filter { it != senderUid }
        Log.d("FCM", "Members for classroom $classroomId (excluding sender $senderUid): $memberUids")
        for (uid in memberUids) {
            val tokenSnapshot = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("fcmToken").get().await()
            val token = tokenSnapshot.getValue(String::class.java)
            Log.d("FCM", "Token for UID $uid: $token")
            if (token != null) {
                val title = "$announcerName created an assignment"
                val body = "$announcerName created an assignment $assignmentName"
                sendNotificationToToken(token, title, body, classroomId, assignmentId, uid)
            }
        }
    }


}