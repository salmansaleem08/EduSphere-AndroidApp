package com.salmansaleem.edusphere

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class CreateClass : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private var selectedBitmap: Bitmap? = null
    private var localImagePath: String? = null
    private lateinit var classRoomImageView: ImageView
    private val TAG = "CreateClass"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_class)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Classrooms")
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")
        databaseHelper = DatabaseHelper(this)

        // Initialize Retrofit with logging
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(IP.baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        apiService = retrofit.create(ApiService::class.java)



        // UI Elements
        val backIcon = findViewById<ImageView>(R.id.iv_back)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val sectionEditText = findViewById<EditText>(R.id.et_section)
        val roomEditText = findViewById<EditText>(R.id.et_room)
        val subjectEditText = findViewById<EditText>(R.id.et_subject)
        val classCodeEditText = findViewById<EditText>(R.id.et_class_code)
        val createButton = findViewById<Button>(R.id.btn_create)
        classRoomImageView = findViewById<ImageView>(R.id.iv_classroom_icon)
        val titleText = findViewById<TextView>(R.id.tv_title)

        // Set unique class code
        classCodeEditText.setText(generateClassCode())

        // Load placeholder image
        Picasso.get()
            .load(R.drawable.gcr)
            .into(classRoomImageView)

        // Back button
        backIcon.setOnClickListener {
            finish()
        }

        // Classroom image click to select from gallery
        classRoomImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePicker.launch(intent)
        }

        // Create button
        createButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val section = sectionEditText.text.toString().trim()
            val room = roomEditText.text.toString().trim()
            val subject = subjectEditText.text.toString().trim()
            val classCode = classCodeEditText.text.toString().trim()

            if (validateInputs(name, subject, classCode)) {
                val classroomId = UUID.randomUUID().toString()
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                    createButton.isEnabled = true
                    return@setOnClickListener
                }
                val uid = currentUser.uid
                fetchInstructorName(uid) { instructorName ->
                    createClassroom(classroomId, uid, name, section, room, subject, classCode, instructorName)
                }
            }
        }

        // Schedule periodic sync worker
        scheduleSyncWorker()
    }





    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    selectedBitmap = BitmapFactory.decodeStream(inputStream)
                    selectedBitmap?.let {
                        classRoomImageView.setImageBitmap(it)
                        // Save image locally for offline use
                        localImagePath = saveImageLocally(it, UUID.randomUUID().toString())
                    }
                }
            }
        }
    }





    private fun generateClassCode(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8)
    }

    private fun validateInputs(name: String, subject: String, classCode: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Classroom name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (subject.isEmpty()) {
            Toast.makeText(this, "Subject is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (classCode.length != 8) {
            Toast.makeText(this, "Class code must be 8 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
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

    private fun saveImageLocally(bitmap: Bitmap, classroomId: String): String {
        val fileName = "${classroomId}_classroom.png"
        val file = File(filesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "Image saved locally at: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image locally: ${e.message}", e)
            Toast.makeText(this, "Failed to save image locally", Toast.LENGTH_SHORT).show()
        }
        return file.absolutePath
    }

    private fun fetchInstructorName(uid: String, callback: (String) -> Unit) {
        if (isOnline()) {
            userDatabase.child(uid).child("fullName").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java) ?: "Unknown"
                    callback(name)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to fetch instructor name: ${error.message}")
                    val localUser = databaseHelper.getUserProfile(uid)
                    callback(localUser?.get("name") ?: "Unknown")
                }
            })
        } else {
            val localUser = databaseHelper.getUserProfile(uid)
            callback(localUser?.get("name") ?: "Unknown")
        }
    }

    private fun createClassroom(
        classroomId: String,
        uid: String,
        name: String,
        section: String,
        room: String,
        subject: String,
        classCode: String,
        instructorName: String
    ) {
        val createButton = findViewById<Button>(R.id.btn_create)
        createButton.isEnabled = false


        // Save selected image locally (for both online and offline cases)
        if (selectedBitmap != null && localImagePath == null) {
            localImagePath = saveImageLocally(selectedBitmap!!, classroomId)
        }

        // Update SQLite with local image path as fallback
        // In createClassroom function, after successful image upload
        databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)


//        databaseHelper.insertClassroom(
//            classroomId, uid, name, section, room, subject, classCode, null, instructorName
//        )
        // Fetch image to ensure it’s saved locally
       // fetchClassroomImage(classroomId, name, instructorName, uid, section, room, subject, classCode, null)


        if (isOnline()) {
            // Update Firebase
            val classroomData = mapOf(
                "name" to name,
                "section" to section,
                "room" to room,
                "subject" to subject,
                "class_code" to classCode,
                "uid" to uid,
                "instructor_name" to instructorName
            )
            database.child(classroomId).setValue(classroomData)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Firebase create error: ${task.exception?.message}")
                        Toast.makeText(this, "Database error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        // Queue for sync
                        databaseHelper.queueClassroomUpdate(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
                    }
                }

            // Upload image if selected
            if (selectedBitmap != null) {
                uploadImageToServer(classroomId, selectedBitmap!!, name, section, room, subject, classCode, uid, instructorName)
            } else {
                Toast.makeText(this, "Classroom created successfully", Toast.LENGTH_SHORT).show()
                createButton.isEnabled = true
                finish()
            }
        } else {

            databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)

            // Queue for sync when online

            databaseHelper.queueClassroomUpdate(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
            Toast.makeText(this, "Classroom created locally, will sync when online", Toast.LENGTH_SHORT).show()
            createButton.isEnabled = true
            finish()
        }
    }



    private fun uploadImageToServer(
        classroomId: String,
        bitmap: Bitmap,
        name: String,
        section: String,
        room: String,
        subject: String,
        classCode: String,
        uid: String,
        instructorName: String
    ) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "${classroomId}_classroom.png",
            RequestBody.create("image/png".toMediaType(), byteArray)
        )
        val classroomIdPart = RequestBody.create("text/plain".toMediaType(), classroomId)

        apiService.uploadClassroomImage(classroomIdPart, imagePart).enqueue(object : Callback<ClassroomImageResponse> {
            override fun onResponse(call: Call<ClassroomImageResponse>, response: Response<ClassroomImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "Image uploaded successfully")
                    // Save the uploaded image locally
                    localImagePath = saveImageLocally(selectedBitmap!!, classroomId)
                    databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
                    Toast.makeText(this@CreateClass, "Classroom created successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e(TAG, "Image upload failed: ${response.code()} ${response.message()}")
                    databaseHelper.queueClassroomUpdate(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
                    Toast.makeText(this@CreateClass, "Image upload failed, queued for sync", Toast.LENGTH_SHORT).show()
                    finish()
                }
                findViewById<Button>(R.id.btn_create).isEnabled = true
            }

            override fun onFailure(call: Call<ClassroomImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error uploading image: ${t.message}", t)
                databaseHelper.queueClassroomUpdate(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
                Toast.makeText(this@CreateClass, "Network error, classroom saved locally and queued for sync", Toast.LENGTH_SHORT).show()
                finish()
                findViewById<Button>(R.id.btn_create).isEnabled = true
            }
        })
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("classroom_sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }
}