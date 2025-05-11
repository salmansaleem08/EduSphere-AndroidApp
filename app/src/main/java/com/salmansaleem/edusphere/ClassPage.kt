package com.salmansaleem.edusphere

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Base64
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.view.View
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.database.ktx.getValue
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm


class ClassPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var announcementRecyclerView: RecyclerView
    private val announcements = mutableListOf<Announcement>()
    private lateinit var announcementAdapter: AnnouncementAdapter
    private lateinit var classroomId: String
    private lateinit var uid: String
    private lateinit var userName: String
    private val TAG = "ClassPage"
    private var isTeacher = false
    private lateinit var apiService: ApiService
    private lateinit var titleTextView: TextView

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_page)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Announcements")
        databaseHelper = DatabaseHelper(this)


        val retrofit = retrofit2.Retrofit.Builder()
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
        apiService = retrofit.create(ApiService::class.java)

        // Get classroom ID and name from intent
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        val classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"
        uid = auth.currentUser?.uid ?: ""

        // Initialize UI
        titleTextView = findViewById<TextView>(R.id.tv_title)
        val announceEditText = findViewById<EditText>(R.id.et_announce)
        val sendButton = findViewById<ImageView>(R.id.iv_send)
        val backButton = findViewById<ImageView>(R.id.iv_back)
        announcementRecyclerView = findViewById<RecyclerView>(R.id.tv_announcement)

        // Set classroom name
        titleTextView.text = classroomName

        // Get user name
        databaseHelper.getUserProfile(uid)?.let { user ->
            userName = user["name"] ?: "User"
        } ?: run {
            userName = "User"
        }





//        databaseHelper.getClassroom(classroomId)?.let { classroom ->
//            isTeacher = classroom["uid"] == uid
//            updateButtonVisibility()
//        } ?: run {
//            if (isOnline()) {
//                FirebaseDatabase.getInstance().getReference("Classrooms").child(classroomId)
//                    .addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            val instructorUid = snapshot.child("uid").getValue(String::class.java) ?: ""
//                            isTeacher = instructorUid == uid
//                            updateButtonVisibility()
//                        }
//
//                        override fun onCancelled(error: DatabaseError) {
//                            Log.e(TAG, "Firebase classroom error: ${error.message}")
//                            isTeacher = false
//                            updateButtonVisibility()
//                        }
//                    })
//            } else {
//                isTeacher = false
//                updateButtonVisibility()
//            }
//        }
        if (isOnline()) {
            FirebaseDatabase.getInstance().getReference("Classrooms").child(classroomId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val instructorUid = snapshot.child("uid").getValue(String::class.java) ?: ""
                        isTeacher = instructorUid == uid
                        updateButtonVisibility()
                        Log.d(TAG, "Firebase check for classroom $classroomId: isTeacher = $isTeacher")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase classroom error: ${error.message}")
                        // Fallback to local database
                        databaseHelper.getClassroom(classroomId)?.let { classroom ->
                            isTeacher = classroom["uid"] == uid
                            updateButtonVisibility()
                            Log.d(TAG, "Fallback to local database for classroom $classroomId: isTeacher = $isTeacher")
                        } ?: run {
                            isTeacher = false
                            updateButtonVisibility()
                            Log.d(TAG, "No local data for classroom $classroomId, setting isTeacher = false")
                        }
                    }
                })
        } else {
            // Offline: Check local database
            databaseHelper.getClassroom(classroomId)?.let { classroom ->
                isTeacher = classroom["uid"] == uid
                updateButtonVisibility()
                Log.d(TAG, "Offline: Local database check for classroom $classroomId: isTeacher = $isTeacher")
            } ?: run {
                isTeacher = false
                updateButtonVisibility()
                Log.d(TAG, "Offline: No local data for classroom $classroomId, setting isTeacher = false")
            }
        }

        // Setup RecyclerView
        announcementAdapter = AnnouncementAdapter(announcements){}
        announcementRecyclerView.layoutManager = LinearLayoutManager(this)
        announcementRecyclerView.adapter = announcementAdapter

        // Load announcements
        loadAnnouncements()

        // Send announcement
        sendButton.setOnClickListener {
            val text = announceEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                postAnnouncement()
                announceEditText.text.clear()
            } else {
                Toast.makeText(this, "Please enter an announcement", Toast.LENGTH_SHORT).show()
            }
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }


        announcementAdapter = AnnouncementAdapter(announcements) { announcement ->
            val intent = Intent(this, AnnouncementPage::class.java).apply {
                putExtra("announcement_id",announcement.announcementId)
                putExtra("classroom_id", announcement.classroomId)
                putExtra("classroom_name", titleTextView.text.toString())
                putExtra("teacher_name", announcement.name)
                putExtra("announcement_text", announcement.text)
                putExtra("timestamp", announcement.timestamp)
                putExtra("uid", announcement.uid)
            }
            startActivity(intent)
        }
        announcementRecyclerView.layoutManager = LinearLayoutManager(this)
        announcementRecyclerView.adapter = announcementAdapter


        // Navigate to ClassFellows
        val classFellowsButton = findViewById<ImageView>(R.id.iv_group) // Assumed ID, update as needed
        classFellowsButton.setOnClickListener {
            val intent = Intent(this, ClassFellows::class.java).apply {
                putExtra("classroom_id", classroomId)
                putExtra("classroom_name", titleTextView.text.toString())
            }
            startActivity(intent)
        }


//        val taskUpload = findViewById<ImageView>(R.id.iv_completed) // Assumed ID, update as needed
//        taskUpload.setOnClickListener {
//            val intent = Intent(this, CreateAssignment::class.java).apply {
//                putExtra("classroom_id", classroomId)
//                putExtra("classroom_name", titleTextView.text.toString())
//            }
//            startActivity(intent)
//        }
//
//        val taskList = findViewById<ImageView>(R.id.iv_todo) // Assumed ID, update as needed
//        taskList.setOnClickListener {
//            val intent = Intent(this, ClassTasks::class.java).apply {
//                putExtra("classroom_id", classroomId)
//                putExtra("classroom_name", titleTextView.text.toString())
//            }
//            startActivity(intent)
//        }


        val taskUpload = findViewById<ImageView>(R.id.iv_completed)
        taskUpload.setOnClickListener {
            if (isTeacher) {
                val intent = Intent(this, CreateAssignment::class.java).apply {
                    putExtra("classroom_id", classroomId)
                    putExtra("classroom_name", titleTextView.text.toString())
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Only teachers can upload tasks", Toast.LENGTH_SHORT).show()
            }
        }

        val taskList = findViewById<ImageView>(R.id.iv_todo)
        taskList.setOnClickListener {
            if (!isTeacher) {
                val intent = Intent(this, ClassTasks::class.java).apply {
                    putExtra("classroom_id", classroomId)
                    putExtra("classroom_name", titleTextView.text.toString())
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Only students can view task list", Toast.LENGTH_SHORT).show()
            }
        }

        // Call updateButtonVisibility after setting up click listeners
        updateButtonVisibility()
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }


    private fun postAnnouncement() {
        val announceEditText = findViewById<EditText>(R.id.et_announce)
        val announcementText = announceEditText.text.toString().trim()
        if (announcementText.isNotEmpty()) {
            val announcementId = UUID.randomUUID().toString()
            val classroomId = this.classroomId
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val userName = this.userName
            val timestamp = SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
            val profileImagePath = userId?.let { databaseHelper.getUserProfile(it)?.get("profile_image_path") }

            if (userId != null && classroomId != null) {
                if (isOnline()) {
                    // Online: Save to Firebase and send notifications
                    val announcementData = mapOf(
                        "uid" to userId,
                        "name" to userName,
                        "text" to announcementText,
                        "timestamp" to timestamp
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("Announcements")
                        .child(classroomId)
                        .child(announcementId)
                        .setValue(announcementData)
                        .addOnSuccessListener {
                            // Fetch className for notification
                            FirebaseDatabase.getInstance().reference
                                .child("Classrooms")
                                .child(classroomId)
                                .child("name")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val className = snapshot.getValue(String::class.java) ?: "Classroom"
                                    CoroutineScope(Dispatchers.Main).launch {
                                        sendAnnouncementNotification(
                                            classroomId,
                                            announcementId,
                                            userName,
                                            className,
                                            userId, // Pass senderUid
                                            announcementText // Pass announcementText
                                        )
                                    }
                                }
                            databaseHelper.insertAnnouncement(announcementId, classroomId, userId, userName, announcementText, timestamp)
                            val announcement = Announcement(announcementId, classroomId, userId, userName, announcementText, timestamp, profileImagePath)
                            announcements.add(announcement)
                            announcements.sortByDescending { it.timestamp }
                            announcementAdapter.notifyDataSetChanged()
                            announceEditText.text.clear()
                            Toast.makeText(this, "Announcement posted", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to post announcement", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Offline: Queue for sync
                    databaseHelper.queueAnnouncementUpdate(announcementId, classroomId, userId, userName, announcementText, timestamp)
                    databaseHelper.insertAnnouncement(announcementId, classroomId, userId, userName, announcementText, timestamp)
                    val announcement = Announcement(announcementId, classroomId, userId, userName, announcementText, timestamp, profileImagePath)
                    announcements.add(announcement)
                    announcements.sortByDescending { it.timestamp }
                    announcementAdapter.notifyDataSetChanged()
                    val workRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                    WorkManager.getInstance(this).enqueue(workRequest)
                    Toast.makeText(this, "Announcement saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Announcement cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }






    private fun loadAnnouncements() {
        announcements.clear()
        if (isOnline()) {
            database.child(classroomId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasData = false
                    for (announcementSnapshot in snapshot.children) {
                        hasData = true
                        val announcementId = announcementSnapshot.key ?: continue
                        val uid = announcementSnapshot.child("uid").getValue(String::class.java) ?: ""
                        val name = announcementSnapshot.child("name").getValue(String::class.java) ?: ""
                        val text = announcementSnapshot.child("text").getValue(String::class.java) ?: ""
                        val timestamp = announcementSnapshot.child("timestamp").getValue(String::class.java) ?: ""
                        databaseHelper.insertAnnouncement(announcementId, classroomId, uid, name, text, timestamp)
                        fetchProfileImage(uid, name, announcementId, classroomId, text, timestamp)
                    }
                    if (!hasData) {
                        loadLocalAnnouncements()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase database error: ${error.message}")
                    loadLocalAnnouncements()
                }
            })
        } else {
            loadLocalAnnouncements()
        }
    }



    private fun loadLocalAnnouncements() {
        val localAnnouncements = databaseHelper.getAnnouncements(classroomId)
        for (announcementData in localAnnouncements) {
            val announcementId = announcementData["announcement_id"] ?: continue
            val classroomId = announcementData["classroom_id"] ?: ""
            val uid = announcementData["uid"] ?: ""
            val name = announcementData["name"] ?: ""
            val text = announcementData["announcement_text"] ?: ""
            val timestamp = announcementData["timestamp"] ?: ""
            val profileImagePath = databaseHelper.getUserProfile(uid)?.get("profile_image_path")
            val announcement = Announcement(announcementId, classroomId, uid, name, text, timestamp, profileImagePath)
            if (!announcements.any { it.announcementId == announcementId }) {
                announcements.add(announcement)
            }
        }
        announcements.sortByDescending { it.timestamp }
        announcementAdapter.notifyDataSetChanged()
    }


    private fun fetchProfileImage(uid: String, name: String, announcementId: String, classroomId: String, text: String, timestamp: String) {
        val localFile = File(filesDir, "${uid}_profile.png")
        if (localFile.exists()) {
            val announcement = Announcement(announcementId, classroomId, uid, name, text, timestamp, localFile.absolutePath)
            if (!announcements.any { it.announcementId == announcementId }) {
                announcements.add(announcement)
                announcements.sortByDescending { it.timestamp }
                announcementAdapter.notifyDataSetChanged()
                Log.d(TAG, "Used existing local profile image for $uid: ${localFile.absolutePath}")
            }
            return
        }

        val request = FetchImageRequest(uid)
        apiService.fetchProfileImage(request).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                var localImagePath: String? = null
                if (response.isSuccessful && response.body()?.success == true) {
                    val imageUrl = response.body()?.image_url
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d(TAG, "Fetched image URL for $uid: $imageUrl")
                        Picasso.get().load(imageUrl).into(object : Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    localImagePath = saveImageLocally(bitmap, uid)
                                    Log.d(TAG, "Image loaded and saved for $uid: $localImagePath")
                                    if (localImagePath != null) {
                                        databaseHelper.updateUserProfileImage(uid, localImagePath)
                                    }
                                    val announcement = Announcement(announcementId, classroomId, uid, name, text, timestamp, localImagePath)
                                    if (!announcements.any { it.announcementId == announcementId }) {
                                        announcements.add(announcement)
                                        announcements.sortByDescending { it.timestamp }
                                        announcementAdapter.notifyDataSetChanged()
                                        Log.d(TAG, "Added announcement $announcementId with localImagePath: $localImagePath")
                                    }
                                } else {
                                    Log.w(TAG, "Bitmap is null for $uid")
                                    addAnnouncementWithNoImage(announcementId, classroomId, uid, name, text, timestamp)
                                }
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.e(TAG, "Picasso failed to load image for $uid: ${e?.message}", e)
                                addAnnouncementWithNoImage(announcementId, classroomId, uid, name, text, timestamp)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                        })
                    } else {
                        Log.w(TAG, "No image URL provided for $uid")
                        addAnnouncementWithNoImage(announcementId, classroomId, uid, name, text, timestamp)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed for $uid: ${response.code()} ${response.message()}")
                    addAnnouncementWithNoImage(announcementId, classroomId, uid, name, text, timestamp)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image for $uid: ${t.message}", t)
                addAnnouncementWithNoImage(announcementId, classroomId, uid, name, text, timestamp)
            }
        })
    }

    private fun addAnnouncementWithNoImage(announcementId: String, classroomId: String, uid: String, name: String, text: String, timestamp: String) {
        val announcement = Announcement(announcementId, classroomId, uid, name, text, timestamp, null)
        if (!announcements.any { it.announcementId == announcementId }) {
            announcements.add(announcement)
            announcements.sortByDescending { it.timestamp }
            announcementAdapter.notifyDataSetChanged()
            Log.d(TAG, "Added announcement $announcementId with no image")
        }
    }

    private fun saveImageLocally(bitmap: Bitmap, uid: String): String? {
        val fileName = "${uid}_profile.png"
        val file = File(filesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "Profile image saved locally at: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "IO error saving profile image for $uid: ${e.message}", e)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error saving profile image for $uid: ${e.message}", e)
            return null
        }
    }


    private fun updateButtonVisibility() {
        val taskUpload = findViewById<ImageView>(R.id.iv_completed)
        val taskList = findViewById<ImageView>(R.id.iv_todo)
        taskUpload.visibility = if (isTeacher) View.VISIBLE else View.GONE
        taskList.visibility = if (!isTeacher) View.VISIBLE else View.GONE
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
            val inputStream: InputStream = resources.openRawResource(R.raw.service_account)
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





}