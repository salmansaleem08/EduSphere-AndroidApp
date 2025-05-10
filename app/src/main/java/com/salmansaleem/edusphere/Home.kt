package com.salmansaleem.edusphere

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Typeface
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.TimeUnit

class Home : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private val TAG = "Home"
    private lateinit var classRecyclerView: RecyclerView
    private val classrooms = mutableListOf<Classroom>()


    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var filterCompleted: TextView
    private lateinit var filterToday: TextView
    private lateinit var filterUpcoming: TextView
    private lateinit var filterMissing: TextView
    private var currentFilter = "Completed"
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Classrooms")
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")
        databaseHelper = DatabaseHelper(this)

        // Initialize Retrofit
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
        val settingsIcon = findViewById<ImageView>(R.id.iv_settings)
        val addClassButton = findViewById<ImageButton>(R.id.btn_add_class)
        val userProfileImage = findViewById<ImageView>(R.id.iv_user_profile)
        val greetingText = findViewById<TextView>(R.id.tv_greeting)
        classRecyclerView = findViewById<RecyclerView>(R.id.rv_my_classes)
        val recyclerViewTasks = findViewById<RecyclerView>(R.id.rv_tasks)
        filterCompleted = findViewById<TextView>(R.id.text1)
        filterToday = findViewById<TextView>(R.id.text2)
        filterUpcoming = findViewById<TextView>(R.id.text3)
        filterMissing = findViewById<TextView>(R.id.text4)

        // Setup Class RecyclerView
        classRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        classRecyclerView.adapter = ClassAdapter(classrooms) { classroom ->
            val intent = Intent(this, ClassPage::class.java).apply {
                putExtra("classroom_id", classroom.classroomId)
                putExtra("classroom_name", classroom.name)
            }
            startActivity(intent)
        }

        // Setup Tasks RecyclerView
        tasksAdapter = TasksAdapter(mutableListOf())
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = tasksAdapter

        // Set filter listeners
        filterCompleted.setOnClickListener { setFilter("Completed") }
        filterToday.setOnClickListener { setFilter("Today") }
        filterUpcoming.setOnClickListener { setFilter("Upcoming") }
        filterMissing.setOnClickListener { setFilter("Missing") }

        // Check authentication
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, Login::class.java))
            finish()
            return
        }
        val uid = currentUser.uid

        // Load user data and classes
        loadUserData(uid, userProfileImage, greetingText)
        loadClassrooms(uid)

        // Settings button
        settingsIcon.setOnClickListener {
            startActivity(Intent(this, EditProfile::class.java))
        }

        // Add class button
        addClassButton.setOnClickListener {
            showClassOptionsDialog(uid)
        }

        // Initialize tasks with default filter
        setFilter("Completed")
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

    private fun loadUserData(uid: String, profileImageView: ImageView, greetingText: TextView) {
        if (isOnline()) {
            userDatabase.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java) ?: "User"
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                    greetingText.text = "Hi, $name\nWelcome back"
                    databaseHelper.updateUserProfile(uid, name, bio, phone)
                    fetchProfileImage(uid, profileImageView)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase database error: ${error.message}")
                    loadUserFromSQLite(uid, profileImageView, greetingText)
                }
            })
        } else {
            loadUserFromSQLite(uid, profileImageView, greetingText)
        }
    }

    private fun loadUserFromSQLite(uid: String, profileImageView: ImageView, greetingText: TextView) {
        val user = databaseHelper.getUserProfile(uid)
        if (user != null) {
            val name = user["name"] ?: "User"
            greetingText.text = "Hi, $name\nWelcome back"

            val localFile = File(filesDir, "${uid}_profile.png")
            if (localFile.exists()) {
                Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.user_profile_placeholder)
                    .error(R.drawable.user_profile_placeholder)
                    .into(profileImageView)
                Log.d(TAG, "Loaded local profile image: ${localFile.absolutePath}")
            } else {
                Picasso.get()
                    .load(R.drawable.user_profile_placeholder)
                    .into(profileImageView)
                Log.w(TAG, "No local profile image found for uid $uid")
            }
        } else {
            greetingText.text = "Hi, User\nWelcome back"
            Picasso.get()
                .load(R.drawable.user_profile_placeholder)
                .into(profileImageView)
            Log.w(TAG, "No user profile found in SQLite for uid $uid")
        }
    }

    private fun fetchProfileImage(uid: String, profileImageView: ImageView) {
        val request = FetchImageRequest(uid)
        apiService.fetchProfileImage(request).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.image_url?.let { url ->
                        Log.d(TAG, "Fetched profile image URL: $url")
                        Picasso.get()
                            .load(url)
                            .placeholder(R.drawable.user_profile_placeholder)
                            .error(R.drawable.user_profile_placeholder)
                            .into(profileImageView)
                        // Update SQLite with image_url
                        // databaseHelper.updateUserProfileImageUrl(uid, url)
                    } ?: run {
                        Log.w(TAG, "No profile image URL in response")
                        loadLocalProfileImage(uid, profileImageView)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed: ${response.code()} ${response.message()}")
                    loadLocalProfileImage(uid, profileImageView)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image: ${t.message}", t)
                loadLocalProfileImage(uid, profileImageView)
            }
        })
    }

    private fun loadLocalProfileImage(uid: String, profileImageView: ImageView) {
        val user = databaseHelper.getUserProfile(uid)
        val imageUrl = user?.get("profile_image_url")
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(profileImageView)
            Log.d(TAG, "Loaded profile image from URL: $imageUrl")
        } else {
            val localFile = File(filesDir, "${uid}_profile.png")
            if (localFile.exists()) {
                Picasso.get()
                    .load(localFile)
                    .placeholder(R.drawable.user_profile_placeholder)
                    .error(R.drawable.user_profile_placeholder)
                    .into(profileImageView)
                Log.d(TAG, "Loaded local profile image: ${localFile.absolutePath}")
            } else {
                Picasso.get()
                    .load(R.drawable.user_profile_placeholder)
                    .into(profileImageView)
                Log.w(TAG, "No local profile image or URL found for uid $uid")
            }
        }
    }




    private fun saveImageLocally(bitmap: Bitmap, id: String): String {
        val fileName = "${id}_classroom.png"
        val file = File(applicationContext.filesDir, fileName)
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

    private fun loadLocalClassroom(classroomId: String) {
        val classroomData = databaseHelper.getClassroom(classroomId)
        if (classroomData != null) {
            val imageUrl = classroomData["image_url"]
            val classroom = Classroom(
                classroomId,
                classroomData["name"] ?: "",
                classroomData["instructor_name"] ?: "",
                imageUrl
            )
            if (!classrooms.any { it.classroomId == classroomId }) {
                Log.d(TAG, "Loaded local classroom $classroomId with image URL: $imageUrl")
                classrooms.add(classroom)
                classRecyclerView.adapter?.notifyDataSetChanged()
            }
        } else {
            Log.w(TAG, "No local data found for classroom $classroomId")
        }
    }




    private fun showClassOptionsDialog(uid: String) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Class Options")
            .setItems(arrayOf("Join Class", "Create Class")) { _, which ->
                when (which) {
                    0 -> showJoinClassDialog(uid) // Join Class
                    1 -> startActivity(Intent(this, CreateClass::class.java)) // Create Class
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun showJoinClassDialog(uid: String) {
        val input = EditText(this)
        input.hint = "Enter Class Code"
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Join Class")
            .setView(input)
            .setPositiveButton("Join") { _, _ ->
                val classCode = input.text.toString().trim()
                if (classCode.isNotEmpty()) {
                    joinClass(classCode, uid)
                } else {
                    Toast.makeText(this, "Please enter a class code", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun joinClass(classCode: String, uid: String) {
        if (isOnline()) {
            database.orderByChild("class_code").equalTo(classCode)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val classroomSnapshot = snapshot.children.first()
                            val classroomId = classroomSnapshot.key ?: return
                            val name = classroomSnapshot.child("name").getValue(String::class.java) ?: ""
                            val section = classroomSnapshot.child("section").getValue(String::class.java) ?: ""
                            val room = classroomSnapshot.child("room").getValue(String::class.java) ?: ""
                            val subject = classroomSnapshot.child("subject").getValue(String::class.java) ?: ""
                            val classCode = classroomSnapshot.child("class_code").getValue(String::class.java) ?: ""
                            val instructorName = classroomSnapshot.child("instructor_name").getValue(String::class.java) ?: ""
                            // Add user to Firebase Classes table
                            FirebaseDatabase.getInstance().getReference("Classes").child(classroomId).child("members").child(uid).setValue(true)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        // Add to SQLite
                                        databaseHelper.addClassMember(classroomId, uid)
                                        // Insert classroom data into SQLite
                                        databaseHelper.insertClassroom(
                                            classroomId, uid, name, section, room, subject, classCode, null, instructorName
                                        )
                                        // Fetch image to ensure it’s saved locally
                                        fetchClassroomImage(classroomId, name, instructorName, uid, section, room, subject, classCode, null)
                                        Toast.makeText(this@Home, "Joined class successfully", Toast.LENGTH_SHORT).show()
                                        loadClassrooms(uid) // Refresh classroom list
                                    } else {
                                        Toast.makeText(this@Home, "Failed to join class: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this@Home, "Invalid class code", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error joining class: ${error.message}")
                        Toast.makeText(this@Home, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            val classroomId = databaseHelper.joinClassByCode(classCode, uid)
            if (classroomId != null) {
                Toast.makeText(this, "Joined class locally, will sync when online", Toast.LENGTH_SHORT).show()
                loadClassrooms(uid) // Refresh classroom list
            } else {
                Toast.makeText(this, "Invalid class code", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun loadClassrooms(uid: String) {
        classrooms.clear()
        if (isOnline()) {
            // Step 1: Fetch classrooms created by the user
            database.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasData = false
                    for (classSnapshot in snapshot.children) {
                        hasData = true
                        val classroomId = classSnapshot.key ?: continue
                        val name = classSnapshot.child("name").getValue(String::class.java) ?: ""
                        val section = classSnapshot.child("section").getValue(String::class.java) ?: ""
                        val room = classSnapshot.child("room").getValue(String::class.java) ?: ""
                        val subject = classSnapshot.child("subject").getValue(String::class.java) ?: ""
                        val classCode = classSnapshot.child("class_code").getValue(String::class.java) ?: ""
                        val instructorName = classSnapshot.child("instructor_name").getValue(String::class.java) ?: ""
                        // Get image path from SQLite
                        val classroomData = databaseHelper.getClassroom(classroomId)
                        val imagePath = classroomData?.get("image_path")
//                        // Insert or update SQLite
//                        databaseHelper.insertClassroom(
//                            classroomId, uid, name, section, room, subject, classCode, imagePath, instructorName
//                        )
//                        // Fetch image
//                        fetchClassroomImage(classroomId, name, instructorName, uid, section, room, subject, classCode, imagePath)

                        databaseHelper.insertClassroom(
                            classroomId, uid, name, section, room, subject, classCode, null, instructorName
                        )
                        // Fetch image to ensure it’s saved locally
                        fetchClassroomImage(classroomId, name, instructorName, uid, section, room, subject, classCode, null)

                        //Toast.makeText(this@Home, "Classroom $name loaded", Toast.LENGTH_SHORT).show()
                    }
                    // Step 2: Fetch classrooms the user has joined
                    FirebaseDatabase.getInstance().getReference("Classes")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(classesSnapshot: DataSnapshot) {
                                for (classSnapshot in classesSnapshot.children) {
                                    if (classSnapshot.child("members").child(uid).exists()) {
                                        val classroomId = classSnapshot.key ?: continue
                                        // Skip if already added (created by user)
                                        if (classrooms.any { it.classroomId == classroomId }) continue
                                        // Fetch classroom details
                                        database.child(classroomId).addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(classData: DataSnapshot) {
                                                if (classData.exists()) {
                                                    hasData = true
                                                    val name = classData.child("name").getValue(String::class.java) ?: ""
                                                    val section = classData.child("section").getValue(String::class.java) ?: ""
                                                    val room = classData.child("room").getValue(String::class.java) ?: ""
                                                    val subject = classData.child("subject").getValue(String::class.java) ?: ""
                                                    val classCode = classData.child("class_code").getValue(String::class.java) ?: ""
                                                    val instructorName = classData.child("instructor_name").getValue(String::class.java) ?: ""
                                                    val classroomData = databaseHelper.getClassroom(classroomId)
                                                    val imagePath = classroomData?.get("image_path")
                                                    databaseHelper.insertClassroom(
                                                        classroomId, uid, name, section, room, subject, classCode, imagePath, instructorName
                                                    )
                                                    fetchClassroomImage(classroomId, name, instructorName, uid, section, room, subject, classCode, imagePath)
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e(TAG, "Error fetching joined classroom $classroomId: ${error.message}")
                                            }
                                        })
                                    }
                                }
                                if (!hasData) {
                                    loadLocalClassrooms(uid)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Firebase Classes database error: ${error.message}")
                                loadLocalClassrooms(uid)
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase database error: ${error.message}")
                    loadLocalClassrooms(uid)
                }
            })
        } else {
            loadLocalClassrooms(uid)
        }
    }




    private fun loadLocalClassrooms(uid: String) {
        val classroomIds = databaseHelper.getUserClassrooms(uid)
        for (classroomId in classroomIds) {
            val classroomData = databaseHelper.getClassroom(classroomId)
            if (classroomData != null) {
                val storedImagePath = classroomData["image_path"] ?: ""
                val localFile = File(storedImagePath)
                val imagePath = if (localFile.exists()) storedImagePath else null
                val classroom = Classroom(
                    classroomId,
                    classroomData["name"] ?: "",
                    classroomData["instructor_name"] ?: "",
                    imagePath
                )
                if (!classrooms.any { it.classroomId == classroomId }) {
                    Log.d(TAG, "Loaded local classroom $classroomId with image path: $imagePath")
                    classrooms.add(classroom)
                }
            } else {
                Log.w(TAG, "No local data found for classroom $classroomId")
            }
        }
        classRecyclerView.adapter?.notifyDataSetChanged()
        if (classrooms.isEmpty()) {
            Log.d(TAG, "No local classrooms found for uid $uid")
        }
    }






    public fun fetchClassroomImage(
        classroomId: String,
        name: String,
        instructorName: String,
        uid: String,
        section: String,
        room: String,
        subject: String,
        classCode: String,
        existingImagePath: String?
    ) {
        // Check if local image exists
        val localFile = File(filesDir, "${classroomId}_classroom.png")
        if (localFile.exists()) {
            val localImagePath = localFile.absolutePath
            databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
            val classroom = Classroom(classroomId, name, instructorName, localImagePath)
            if (!classrooms.any { it.classroomId == classroomId }) {
                classrooms.add(classroom)
                classRecyclerView.adapter?.notifyDataSetChanged()
                Log.d(TAG, "Used existing local image for $classroomId: $localImagePath")
            }
            return
        }

        // Fetch from server if no local image
        val request = FetchClassroomImageRequest(classroomId)
        apiService.fetchClassroomImage(request).enqueue(object : Callback<ClassroomImageResponse> {
            override fun onResponse(call: Call<ClassroomImageResponse>, response: Response<ClassroomImageResponse>) {
                var localImagePath: String? = existingImagePath
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.image_url?.let { url ->
                        // Download image in background thread using coroutines
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                                localImagePath = saveImageLocally(bitmap, classroomId)
                                // Switch to main thread to update UI and database
                                withContext(Dispatchers.Main) {
                                    Log.d(TAG, "Saved classroom image locally for $classroomId: $localImagePath")
                                    databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, localImagePath, instructorName)
                                    val classroom = Classroom(classroomId, name, instructorName, localImagePath)
                                    if (!classrooms.any { it.classroomId == classroomId }) {
                                        classrooms.add(classroom)
                                        classRecyclerView.adapter?.notifyDataSetChanged()
                                        Log.d(TAG, "Added classroom $classroomId to list with localImagePath: $localImagePath")
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle errors on main thread
                                withContext(Dispatchers.Main) {
                                    Log.e(TAG, "Error saving image locally for $classroomId: ${e.message}", e)
                                    databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, null, instructorName)
                                    val classroom = Classroom(classroomId, name, instructorName, null)
                                    if (!classrooms.any { it.classroomId == classroomId }) {
                                        classrooms.add(classroom)
                                        classRecyclerView.adapter?.notifyDataSetChanged()
                                        Log.d(TAG, "Added classroom $classroomId to list without image")
                                    }
                                }
                            }
                        }
                    } ?: run {
                        // No image URL provided
                        Log.w(TAG, "No image URL for classroom $classroomId")
                        databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, null, instructorName)
                        val classroom = Classroom(classroomId, name, instructorName, null)
                        if (!classrooms.any { it.classroomId == classroomId }) {
                            classrooms.add(classroom)
                            classRecyclerView.adapter?.notifyDataSetChanged()
                            Log.d(TAG, "Added classroom $classroomId to list without image")
                        }
                    }
                } else {
                    Log.e(TAG, "Fetch classroom image failed for $classroomId: ${response.code()} ${response.message()}")
                    databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, existingImagePath, instructorName)
                    val classroom = Classroom(classroomId, name, instructorName, existingImagePath)
                    if (!classrooms.any { it.classroomId == classroomId }) {
                        classrooms.add(classroom)
                        classRecyclerView.adapter?.notifyDataSetChanged()
                        Log.d(TAG, "Added classroom $classroomId to list with existing localImagePath: $existingImagePath")
                    }
                }
            }

            override fun onFailure(call: Call<ClassroomImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching classroom image for $classroomId: ${t.message}", t)
                databaseHelper.insertClassroom(classroomId, uid, name, section, room, subject, classCode, existingImagePath, instructorName)
                val classroom = Classroom(classroomId, name, instructorName, existingImagePath)
                if (!classrooms.any { it.classroomId == classroomId }) {
                    classrooms.add(classroom)
                    classRecyclerView.adapter?.notifyDataSetChanged()
                    Log.d(TAG, "Added classroom $classroomId to list with existing localImagePath: $existingImagePath")
                }
            }
        })
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun filterAndUpdateTasks(assignments: List<Map<String, String>>) {
        val filteredTasks = assignments.filter { assignment ->
            val dueDateStr = assignment["due_date"] ?: return@filter false
            val submissionId = assignment["submission_id"] ?: ""
            val dueDate = try {
                dateFormat.parse(dueDateStr)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid due date format: $dueDateStr")
                return@filter false
            }
            val currentTime = Calendar.getInstance().time

            val isSubmitted = submissionId.isNotEmpty()
            val isToday = isSameDay(dueDate, currentTime)
            val isBeforeToday = dueDate.before(currentTime) && !isToday
            val isAfterToday = dueDate.after(currentTime) && !isToday

            when (currentFilter) {
                "Completed" -> isSubmitted
                "Today" -> !isSubmitted && isToday
                "Upcoming" -> !isSubmitted && isAfterToday
                "Missing" -> !isSubmitted && isBeforeToday
                else -> false
            }
        }.map { assignment ->
            mapOf(
                "assignment_id" to assignment["assignment_id"]!!,
                "classroom_id" to assignment["classroom_id"]!!,
                "name" to assignment["name"]!!,
                "due_date" to try {
                    val parsedDate = dateFormat.parse(assignment["due_date"]!!)
                    displayDateFormat.format(parsedDate!!)
                } catch (e: Exception) {
                    assignment["due_date"]!!
                }
            )
        }
        tasksAdapter.updateTasks(filteredTasks)
    }

    private fun getUserClassroomIds(uid: String, callback: (List<String>) -> Unit) {
        if (isOnline()) {
            val classroomIds = mutableSetOf<String>()
            FirebaseDatabase.getInstance().getReference("Classes")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (classSnapshot in snapshot.children) {
                            if (classSnapshot.child("members").child(uid).exists()) {
                                classroomIds.add(classSnapshot.key ?: "")
                            }
                        }
                        callback(classroomIds.toList())
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error fetching member classrooms: ${error.message}")
                        callback(emptyList())
                    }
                })
        } else {
            val classroomIds = databaseHelper.getUserClassrooms(uid)
            callback(classroomIds)
        }
    }


    private fun loadTasks() {
        val user = auth.currentUser
        if (user == null) {
            Log.e(TAG, "User not authenticated")
            tasksAdapter.updateTasks(emptyList())
            return
        }
        val userId = user.uid

        getUserClassroomIds(userId) { classroomIds ->
            if (classroomIds.isEmpty()) {
                tasksAdapter.updateTasks(emptyList())
                return@getUserClassroomIds
            }
            if (isOnline()) {
                val allAssignments = mutableListOf<Map<String, String>>()
                var classroomsProcessed = 0
                for (classroomId in classroomIds) {
                    FirebaseDatabase.getInstance().getReference("Assignments").child(classroomId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val assignments = mutableListOf<Map<String, String>>()
                                for (child in snapshot.children) {
                                    val assignment = child.value as? Map<String, Any> ?: continue
                                    assignments.add(
                                        mapOf(
                                            "assignment_id" to (child.key ?: ""),
                                            "classroom_id" to classroomId,
                                            "uid" to (assignment["uid"]?.toString() ?: ""),
                                            "name" to (assignment["name"]?.toString() ?: ""),
                                            "description" to (assignment["description"]?.toString() ?: ""),
                                            "due_date" to (assignment["due_date"]?.toString() ?: ""),
                                            "score" to (assignment["score"]?.toString() ?: "0"),
                                            "submission_id" to ""
                                        )
                                    )
                                }
                                FirebaseDatabase.getInstance().getReference("Submissions").child(classroomId).child(userId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(subSnapshot: DataSnapshot) {
                                            val updatedAssignments = assignments.map { assignment ->
                                                val assignmentId = assignment["assignment_id"]!!
                                                val submission = subSnapshot.child(assignmentId).value as? Map<String, Any>
                                                assignment.toMutableMap().apply {
                                                    put("submission_id", submission?.get("submission_id")?.toString() ?: "")
                                                    put("submitted_at", submission?.get("submitted_at")?.toString() ?: "")
                                                }
                                            }
                                            updatedAssignments.forEach { assignment ->
                                                databaseHelper.insertAssignment(
                                                    assignment["assignment_id"]!!,
                                                    assignment["classroom_id"]!!,
                                                    assignment["uid"]!!,
                                                    assignment["name"]!!,
                                                    assignment["description"]!!,
                                                    assignment["due_date"]!!,
                                                    assignment["score"]!!.toInt(),
                                                    null
                                                )
                                                if (assignment["submission_id"]?.isNotEmpty() == true) {
                                                    databaseHelper.insertSubmission(
                                                        assignment["submission_id"]!!,
                                                        assignment["assignment_id"]!!,
                                                        assignment["classroom_id"]!!,
                                                        userId,
                                                        assignment["submitted_at"]!!,
                                                        null
                                                    )
                                                }
                                            }
                                            allAssignments.addAll(updatedAssignments)
                                            classroomsProcessed++
                                            if (classroomsProcessed == classroomIds.size) {
                                                filterAndUpdateTasks(allAssignments)
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e(TAG, "Firebase submissions fetch error for $classroomId: ${error.message}")
                                            classroomsProcessed++
                                            if (classroomsProcessed == classroomIds.size) {
                                                filterAndUpdateTasks(allAssignments)
                                            }
                                        }
                                    })
                            }
                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Firebase assignments fetch error for $classroomId: ${error.message}")
                                classroomsProcessed++
                                if (classroomsProcessed == classroomIds.size) {
                                    filterAndUpdateTasks(allAssignments)
                                }
                            }
                        })
                }
            } else {
                val assignments = databaseHelper.getAllAssignmentsWithStatus(userId, classroomIds)
                filterAndUpdateTasks(assignments)
            }
        }
    }

    private fun updateFilterUI() {
        val selectedColor = 0xFF000000.toInt() // Black
        val unselectedColor = 0xFF999999.toInt() // Light grey

        filterCompleted.apply {
            setTextColor(if (currentFilter == "Completed") selectedColor else unselectedColor)
            setTypeface(null, if (currentFilter == "Completed") Typeface.BOLD else Typeface.NORMAL)
        }
        filterToday.apply {
            setTextColor(if (currentFilter == "Today") selectedColor else unselectedColor)
            setTypeface(null, if (currentFilter == "Today") Typeface.BOLD else Typeface.NORMAL)
        }
        filterUpcoming.apply {
            setTextColor(if (currentFilter == "Upcoming") selectedColor else unselectedColor)
            setTypeface(null, if (currentFilter == "Upcoming") Typeface.BOLD else Typeface.NORMAL)
        }
        filterMissing.apply {
            setTextColor(if (currentFilter == "Missing") selectedColor else unselectedColor)
            setTypeface(null, if (currentFilter == "Missing") Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        updateFilterUI()
        loadTasks()
    }
}