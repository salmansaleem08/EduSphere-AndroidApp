package com.salmansaleem.edusphere

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val TAG = "SyncWorker"
    private val databaseHelper = DatabaseHelper(appContext)
    private val database = FirebaseDatabase.getInstance()

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
}