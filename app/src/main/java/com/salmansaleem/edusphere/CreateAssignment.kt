package com.salmansaleem.edusphere

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CreateAssignment : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private lateinit var classroomId: String
    private lateinit var classroomName: String
    private var selectedDueDate: String? = null
    private var selectedScore: Int? = null
    private var selectedImageUri: Uri? = null
    private val TAG = "CreateAssignment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_assignment)

        // Initialize DatabaseHelper and ApiService
        databaseHelper = DatabaseHelper(this)
        apiService = retrofit2.Retrofit.Builder()
            .baseUrl(IP.baseUrl)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .client(
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply { level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY })
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .build()
            .create(ApiService::class.java)

        // Get classroom ID and name from intent
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"

        // Initialize UI
        val backButton = findViewById<ImageView>(R.id.iv_back)
        val titleTextView = findViewById<TextView>(R.id.tv_title)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val descriptionEditText = findViewById<EditText>(R.id.et_description)
        val dueDateSection = findViewById<RelativeLayout>(R.id.section_due_date)
        val dueDateLabel = findViewById<TextView>(R.id.tv_due_date_label)
        val scoreSection = findViewById<RelativeLayout>(R.id.section_score)
        val scoreLabel = findViewById<TextView>(R.id.tv_score_label)
        val attachmentSection = findViewById<RelativeLayout>(R.id.section_attachment)
        val attachmentLabel = findViewById<TextView>(R.id.tv_attachment_label)
        val shareButton = findViewById<Button>(R.id.btn_share)



        scheduleSyncWorker()
        // Set title
        titleTextView.text = "Create an Assignment"

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Due date picker
        dueDateSection.setOnClickListener {
            showDateTimePicker(dueDateLabel)
        }

        // Score picker
        scoreSection.setOnClickListener {
            showScorePicker(scoreLabel)
        }

        // Attachment picker
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                // Extract filename or use a default name
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "assignment_image.png"
                attachmentLabel.text = fileName
                Toast.makeText(this, "Image selected: $fileName", Toast.LENGTH_SHORT).show()
            }
        }
        attachmentSection.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Share button
        shareButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter assignment name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDueDate == null) {
                Toast.makeText(this, "Please set due date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedScore == null) {
                Toast.makeText(this, "Please set total score", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAndShareAssignment(name, description)
        }
    }

    private fun showDateTimePicker(dueDateLabel: TextView) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            val timePicker = TimePickerDialog(this, { _, hour, minute ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, day, hour, minute)
                if (selectedDate.timeInMillis < System.currentTimeMillis()) {
                    Toast.makeText(this, "Cannot set past date/time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }
                selectedDueDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(selectedDate.time)
                dueDateLabel.text = selectedDueDate
                Log.d(TAG, "Selected due date: $selectedDueDate")
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
            timePicker.show()
        }, currentYear, currentMonth, currentDay)
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000 // Prevent past dates
        datePicker.show()
    }

    private fun showScorePicker(scoreLabel: TextView) {
        val scores = arrayOf("10", "20", "50", "100")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Total Score")
        builder.setItems(scores) { _, which ->
            selectedScore = scores[which].toInt()
            scoreLabel.text = "Score = $selectedScore"
            Log.d(TAG, "Selected score: $selectedScore")
            Toast.makeText(this, "Score set to ${scores[which]}", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun saveAndShareAssignment(name: String, description: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = user.uid
        val assignmentId = UUID.randomUUID().toString()
        var imagePath: String? = null

        // Save image locally if selected
        if (selectedImageUri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                val file = File(filesDir, "${assignmentId}_assignment.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                imagePath = file.absolutePath
                Log.d(TAG, "Saved assignment image locally at: $imagePath")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image: ${e.message}")
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Save to SQLite
        val inserted = databaseHelper.insertAssignment(
            assignmentId, classroomId, uid, name, description, selectedDueDate!!, selectedScore!!, imagePath
        )
        if (!inserted) {
            Toast.makeText(this, "Failed to save assignment locally", Toast.LENGTH_SHORT).show()
            return
        }

        // Queue for sync
        databaseHelper.queueAssignmentUpdate(
            assignmentId, classroomId, uid, name, description, selectedDueDate!!, selectedScore!!, imagePath
        )

        if (isOnline()) {
            val assignmentData = mapOf(
                "uid" to uid,
                "name" to name,
                "description" to description,
                "due_date" to selectedDueDate,
                "score" to selectedScore,
                "timestamp" to SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
            )
            FirebaseDatabase.getInstance().getReference("Assignments").child(classroomId).child(assignmentId)
                .setValue(assignmentData)
                .addOnSuccessListener {
                    Log.d(TAG, "Assignment synced to Firebase: $assignmentId")
                    databaseHelper.getUserProfile(uid)?.let { user ->
                        val announcerName = user["name"] ?: "User"
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            sendAssignmentNotification(classroomId, assignmentId, announcerName, name, uid)
                        }
                    }
                    if (imagePath != null) {
                        uploadImageToServer(assignmentId, imagePath)
                    } else {
                        Toast.makeText(this, "Assignment shared successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync assignment to Firebase: ${e.message}")
                    Toast.makeText(this, "Assignment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } else {
            Toast.makeText(this, "Assignment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun uploadImageToServer(assignmentId: String, imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) {
            Log.e(TAG, "Image file does not exist: $imagePath")
            Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val imagePart = okhttp3.MultipartBody.Part.createFormData(
            "image",
            "${assignmentId}_assignment.png",
            okhttp3.RequestBody.create("image/png".toMediaType(), byteArray)
        )
        val assignmentIdPart = okhttp3.RequestBody.create("text/plain".toMediaType(), assignmentId)
        apiService.uploadAssignmentImage(assignmentIdPart, imagePart).enqueue(object : Callback<AssignmentImageResponse> {
            override fun onResponse(call: Call<AssignmentImageResponse>, response: Response<AssignmentImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.image_url?.let { url ->
                        try {
                            val bitmapFromServer = android.graphics.BitmapFactory.decodeStream(java.net.URL(url).openStream())
                            val fileName = "${assignmentId}_assignment.png"
                            val newFile = File(filesDir, fileName)
                            FileOutputStream(newFile).use { out ->
                                bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Log.d(TAG, "Saved synced assignment image locally at: ${newFile.absolutePath}")
                            // Update SQLite with synced image path
                            databaseHelper.insertAssignment(
                                assignmentId, classroomId, FirebaseAuth.getInstance().currentUser!!.uid,
                                findViewById<EditText>(R.id.et_name).text.toString().trim(),
                                findViewById<EditText>(R.id.et_description).text.toString().trim(),
                                selectedDueDate!!, selectedScore!!, newFile.absolutePath
                            )
                            Toast.makeText(this@CreateAssignment, "Assignment shared successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving synced image: ${e.message}")
                            Toast.makeText(this@CreateAssignment, "Assignment saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    Log.e(TAG, "Image upload failed: ${response.code()} ${response.message()}")
                    databaseHelper.queueAssignmentUpdate(
                        assignmentId, classroomId, FirebaseAuth.getInstance().currentUser!!.uid,
                        findViewById<EditText>(R.id.et_name).text.toString().trim(),
                        findViewById<EditText>(R.id.et_description).text.toString().trim(),
                        selectedDueDate!!, selectedScore!!, imagePath
                    )
                    Toast.makeText(this@CreateAssignment, "Assignment saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<AssignmentImageResponse>, t: Throwable) {
                Log.e(TAG, "Image upload error: ${t.message}")
                databaseHelper.queueAssignmentUpdate(
                    assignmentId, classroomId, FirebaseAuth.getInstance().currentUser!!.uid,
                    findViewById<EditText>(R.id.et_name).text.toString().trim(),
                    findViewById<EditText>(R.id.et_description).text.toString().trim(),
                    selectedDueDate!!, selectedScore!!, imagePath
                )
                Toast.makeText(this@CreateAssignment, "Assignment saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    private fun scheduleSyncWorker() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("assignment_sync", androidx.work.ExistingPeriodicWorkPolicy.KEEP, syncRequest)
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


    private fun sendNotificationToToken(
        token: String,
        title: String,
        body: String,
        classroomId: String,
        assignmentId: String,
        uid: String
    ) {
        Thread {
            try {
                val accessToken = getAccessToken()
                if (accessToken.isEmpty()) {
                    Log.e("FCM", "Failed to obtain access token")
                    return@Thread
                }
                val projectId = "edusphere-e2107"
                val url = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"
                val json = org.json.JSONObject().apply {
                    put("message", org.json.JSONObject().apply {
                        put("token", token)
                        put("notification", org.json.JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("data", org.json.JSONObject().apply {
                            put("type", "assignment")
                            put("classroomId", classroomId)
                            put("assignmentId", assignmentId)
                        })
                        put("android", org.json.JSONObject().apply {
                            put("priority", "high")
                        })
                    })
                }.toString()
                Log.d("FCM", "Sending JSON to $token: $json")

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("Authorization", "Bearer $accessToken")
                    .build()

                val client = okhttp3.OkHttpClient()
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
            val inputStream: java.io.InputStream = resources.openRawResource(R.raw.service_account)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(jsonString)
            val clientEmail = json.getString("client_email")
            val privateKeyPem = json.getString("private_key")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")
                .trim()
            val tokenUri = json.getString("token_uri")

            Log.d("FCM", "Raw private key (first 50 chars): ${privateKeyPem.take(50)}...")

            val keyBytes = try {
                android.util.Base64.decode(privateKeyPem, android.util.Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e("FCM", "Base64 decode failed: ${e.message}", e)
                return ""
            }
            Log.d("FCM", "Decoded key length: ${keyBytes.size} bytes")

            val keySpec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = java.security.KeyFactory.getInstance("RSA")
            val privateKey = try {
                keyFactory.generatePrivate(keySpec)
            } catch (e: Exception) {
                Log.e("FCM", "Private key parsing failed: ${e.message}", e)
                return ""
            }

            val now = System.currentTimeMillis() / 1000
            val jwt = io.jsonwebtoken.Jwts.builder()
                .setHeaderParam("alg", "RS256")
                .setHeaderParam("typ", "JWT")
                .setIssuer(clientEmail)
                .setAudience(tokenUri)
                .setIssuedAt(java.util.Date(now * 1000))
                .setExpiration(java.util.Date((now + 3600) * 1000))
                .claim("scope", "https://www.googleapis.com/auth/firebase.messaging")
                .signWith(privateKey, io.jsonwebtoken.SignatureAlgorithm.RS256)
                .compact()
            Log.d("FCM", "Generated JWT: $jwt")

            val requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = okhttp3.Request.Builder()
                .url(tokenUri)
                .post(requestBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val client = okhttp3.OkHttpClient()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "{}"
                if (response.isSuccessful) {
                    val responseJson = org.json.JSONObject(responseBody)
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
}
