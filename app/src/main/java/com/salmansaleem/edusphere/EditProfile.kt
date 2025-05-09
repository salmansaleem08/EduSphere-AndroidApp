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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

class EditProfile : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private var selectedBitmap: Bitmap? = null
    private var localImagePath: String? = null
    private lateinit var profileImageView: ImageView
    private val TAG = "EditProfile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
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
        val bioEditText = findViewById<EditText>(R.id.et_username)
        val phoneEditText = findViewById<EditText>(R.id.et_phone)
        val updateButton = findViewById<Button>(R.id.btn_update)
        profileImageView = findViewById<ImageView>(R.id.iv_profile)
        val titleText = findViewById<TextView>(R.id.tv_title)

        // Load user data
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val uid = currentUser.uid
        loadUserData(uid, nameEditText, bioEditText, phoneEditText)

        // Back button
        backIcon.setOnClickListener {
            finish()
        }

        // Profile image click to select from gallery
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePicker.launch(intent)
        }

        // Update button
        updateButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val bio = bioEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            if (validateInputs(name, phone)) {
                updateProfile(uid, name, bio, phone)
            }
        }

        // Schedule periodic sync worker (as a fallback)
        scheduleSyncWorker()
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    selectedBitmap = BitmapFactory.decodeStream(inputStream)
                    selectedBitmap?.let {
                        profileImageView.setImageBitmap(it)
                    }
                }
            }
        }
    }

    private fun validateInputs(name: String, phone: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.isNotEmpty() && (phone.length != 11 || !phone.all { it.isDigit() })) {
            Toast.makeText(this, "Phone number must be 11 digits", Toast.LENGTH_SHORT).show()
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

    private fun saveImageLocally(bitmap: Bitmap, uid: String): String {
        val fileName = "${uid}_profile.png"
        val file = File(filesDir, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "Image saved locally at: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image locally: ${e.message}")
        }
        return file.absolutePath
    }

    private fun loadUserData(uid: String, nameEditText: EditText, bioEditText: EditText, phoneEditText: EditText) {
        if (isOnline()) {
            // Fetch from Firebase
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) ?: ""
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                    nameEditText.setText(name)
                    bioEditText.setText(bio)
                    phoneEditText.setText(phone)

                    // Update SQLite
                    databaseHelper.updateUserProfile(uid, name, bio, phone)

                    // Fetch profile image
                    fetchProfileImage(uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase database error: ${error.message}")
                    Toast.makeText(this@EditProfile, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                    loadFromSQLite(uid, nameEditText, bioEditText, phoneEditText)
                }
            })
        } else {
            loadFromSQLite(uid, nameEditText, bioEditText, phoneEditText)
        }
    }

    private fun loadFromSQLite(uid: String, nameEditText: EditText, bioEditText: EditText, phoneEditText: EditText) {
        val user = databaseHelper.getUserProfile(uid)
        if (user != null) {
            nameEditText.setText(user["name"])
            bioEditText.setText(user["bio"])
            phoneEditText.setText(user["phone"])

            val localFile = File(filesDir, "${uid}_profile.png")
            if (localFile.exists()) {
                Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.user_profile_placeholder)
                    .error(R.drawable.user_profile_placeholder)
                    .into(profileImageView)

                localImagePath = localFile.absolutePath
                Log.d(TAG, "Loaded local image from: $localImagePath")
            } else {
                Picasso.get()
                    .load(R.drawable.user_profile_placeholder)
                    .into(profileImageView)
            }
        } else {
            nameEditText.setText("")
            bioEditText.setText("")
            phoneEditText.setText("")
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(profileImageView)
        }
    }

    private fun fetchProfileImage(uid: String) {
        val request = FetchImageRequest(uid)
        apiService.fetchProfileImage(request).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.image_url?.let { url ->
                        Log.d(TAG, "Fetched image URL: $url")
                        Picasso.get()
                            .load(url)
                            .placeholder(R.drawable.user_profile_placeholder)
                            .error(R.drawable.user_profile_placeholder)
                            .into(profileImageView)
                        try {
                            val bitmap = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                            localImagePath = saveImageLocally(bitmap, uid)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving image locally: ${e.message}")
                        }
                    } ?: run {
                        Log.w(TAG, "No image URL in response")
                        Picasso.get()
                            .load(R.drawable.user_profile_placeholder)
                            .into(profileImageView)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed: ${response.code()} ${response.message()}")
                    loadLocalImage(uid)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image: ${t.message}", t)
                Toast.makeText(this@EditProfile, "Failed to fetch profile image: ${t.message}", Toast.LENGTH_LONG).show()
                loadLocalImage(uid)
            }
        })
    }

    private fun loadLocalImage(uid: String) {
        val localFile = File(filesDir, "${uid}_profile.png")
        if (localFile.exists()) {
            Picasso.get()
                .load(localFile)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(profileImageView)
            localImagePath = localFile.absolutePath
            Log.d(TAG, "Loaded local image from: $localImagePath")
        } else {
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(profileImageView)
        }
    }

    private fun updateProfile(uid: String, name: String, bio: String, phone: String) {
        val updateButton = findViewById<Button>(R.id.btn_update)
        updateButton.isEnabled = false

        // Update SQLite
        databaseHelper.updateUserProfile(uid, name, bio, phone)

         //Save selected image locally
        if (selectedBitmap != null) {
            localImagePath = saveImageLocally(selectedBitmap!!, uid)
        }


        // Save selected image locally
//        if (selectedBitmap != null) {
//            localImagePath = saveImageLocally(selectedBitmap!!, uid)
//            databaseHelper.updateUserProfileImage(uid, localImagePath) // New helper method
//        }


        if (isOnline()) {
            // Update Firebase
            val userData = mapOf(
                "fullName" to name,
                "bio" to bio,
                "phone" to phone
            )
            database.child(uid).updateChildren(userData)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.e(TAG, "Firebase update error: ${task.exception?.message}")
                        Toast.makeText(this, "Database error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            // Upload image if changed
            if (selectedBitmap != null) {
                uploadImageToServer(uid, selectedBitmap!!, name, bio, phone)
            } else {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                updateButton.isEnabled = true
                finish()
            }
        } else {
            // Queue for sync when online
            databaseHelper.queueProfileUpdate(uid, name, bio, phone, localImagePath)
            Toast.makeText(this, "Profile updated locally, will sync when online", Toast.LENGTH_SHORT).show()
            updateButton.isEnabled = true
            finish()
        }
    }

    private fun uploadImageToServer(uid: String, bitmap: Bitmap, name: String, bio: String, phone: String) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "${uid}_profile.png",
            RequestBody.create("image/png".toMediaType(), byteArray)
        )
        val uidPart = RequestBody.create("text/plain".toMediaType(), uid)

        apiService.uploadProfileImage(uidPart, imagePart).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "Image uploaded successfully: ${response.body()?.image_url}")
                    // Save the uploaded image locally
                    response.body()?.image_url?.let { url ->
                        try {
                            val bitmapFromServer = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                            localImagePath = saveImageLocally(bitmapFromServer, uid)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving uploaded image locally: ${e.message}")
                        }
                    }
                    Toast.makeText(this@EditProfile, response.body()?.message ?: "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Log.e(TAG, "Image upload failed: ${response.code()} ${response.message()}")
                    databaseHelper.queueProfileUpdate(uid, name, bio, phone, localImagePath)
                    Toast.makeText(this@EditProfile, "Image upload failed, queued for sync", Toast.LENGTH_SHORT).show()
                    finish()
                }
                findViewById<Button>(R.id.btn_update).isEnabled = true
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error uploading image: ${t.message}", t)
                databaseHelper.queueProfileUpdate(uid, name, bio, phone, localImagePath)
                Toast.makeText(this@EditProfile, "Network error, profile saved locally and queued for sync", Toast.LENGTH_SHORT).show()
                finish()
                findViewById<Button>(R.id.btn_update).isEnabled = true
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
            .enqueueUniquePeriodicWork("profile_sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }
}