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
    private val database = FirebaseDatabase.getInstance().getReference("Users")

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
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
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

            var allSyncedSuccessfully = true

            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val uid = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_UID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)) ?: ""
                val bio = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BIO)) ?: ""
                val phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PHONE)) ?: ""
                val profileImagePath = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROFILE_IMAGE_PATH))

                // Update Firebase
                val userData = mapOf(
                    "fullName" to name,
                    "bio" to bio,
                    "phone" to phone
                )
                var firebaseSuccess = false
                try {
                    database.child(uid).updateChildren(userData).await()
                    firebaseSuccess = true
                    Log.d(TAG, "Firebase synced successfully for uid $uid")
                } catch (e: Exception) {
                    Log.e(TAG, "Firebase sync error for uid $uid: ${e.message}")
                    allSyncedSuccessfully = false
                }

                // Upload image if exists
                var imageSuccess = true // Assume success if no image to upload
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
                                // Save the uploaded image locally
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

                // Delete the processed update only if both Firebase and image sync were successful
                if (firebaseSuccess && imageSuccess) {
                    try {
                        db.delete(
                            DatabaseHelper.TABLE_PROFILE_UPDATES,
                            "${DatabaseHelper.COLUMN_ID} = ?",
                            arrayOf(id.toString())
                        )
                        Log.d(TAG, "Deleted queued update for id $id")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting queued update for id $id: ${e.message}")
                        allSyncedSuccessfully = false
                    }
                }
            }
            cursor.close()
            db.close()

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