package com.salmansaleem.edusphere

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ClassFellows : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var userDatabase: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private lateinit var peopleRecyclerView: RecyclerView
    private val people = mutableListOf<Person>()
    private lateinit var peopleAdapter: PeopleAdapter
    private lateinit var classroomId: String
    private lateinit var classroomName: String
    private val TAG = "ClassFellows"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_fellows)

        // Initialize Firebase and SQLite
        database = FirebaseDatabase.getInstance().getReference("Classes")
        userDatabase = FirebaseDatabase.getInstance().getReference("Users")
        databaseHelper = DatabaseHelper(this)

        // Initialize Retrofit
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
        classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"

        // Initialize UI
        val titleTextView = findViewById<TextView>(R.id.tv_title)
        val backButton = findViewById<ImageView>(R.id.iv_back)
        val searchEditText = findViewById<EditText>(R.id.et_search)
        peopleRecyclerView = findViewById<RecyclerView>(R.id.rv_people)

        // Set classroom name
        titleTextView.text = classroomName

        // Setup RecyclerView
        peopleAdapter = PeopleAdapter(people)
        peopleRecyclerView.layoutManager = LinearLayoutManager(this)
        peopleRecyclerView.adapter = peopleAdapter

        // Load people
        loadPeople()

        // Search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterPeople(query)
            }
        })

        // Back button
        backButton.setOnClickListener {
            finish()
        }
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

    private fun loadPeople() {
        people.clear()
        if (isOnline()) {
            // Fetch teacher from Classrooms
            FirebaseDatabase.getInstance().getReference("Classrooms").child(classroomId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(classroomSnapshot: DataSnapshot) {
                        val teacherUid = classroomSnapshot.child("uid").getValue(String::class.java)
                        if (teacherUid != null) {
                            fetchUserProfile(teacherUid)
                            Log.d(TAG, "Fetched teacher UID: $teacherUid for classroom $classroomId")
                        } else {
                            Log.w(TAG, "No teacher UID found for classroom $classroomId")
                        }

                        // Fetch members from Classes
                        database.child(classroomId).child("members")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(membersSnapshot: DataSnapshot) {
                                    var hasData = false
                                    for (memberSnapshot in membersSnapshot.children) {
                                        hasData = true
                                        val uid = memberSnapshot.key ?: continue
                                        // Avoid fetching teacher again if already fetched
                                        if (uid != teacherUid) {
                                            fetchUserProfile(uid)
                                            Log.d(TAG, "Fetched member UID: $uid for classroom $classroomId")
                                        }
                                    }
                                    if (!hasData && teacherUid == null) {
                                        loadLocalPeople()
                                        Log.d(TAG, "No online members or teacher found, loading local data")
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "Firebase members database error: ${error.message}")
                                    loadLocalPeople()
                                }
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase classrooms database error: ${error.message}")
                        loadLocalPeople()
                    }
                })
        } else {
            loadLocalPeople()
        }
    }

    private fun fetchUserProfile(uid: String) {
        userDatabase.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("fullName").getValue(String::class.java) ?: "User"
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                // Determine if user is the teacher
                var isTeacher = false
                FirebaseDatabase.getInstance().getReference("Classrooms").child(classroomId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(classSnapshot: DataSnapshot) {
                            val teacherUid = classSnapshot.child("uid").getValue(String::class.java)
                            isTeacher = uid == teacherUid
                            // Update SQLite
                            databaseHelper.updateUserProfile(uid, name, bio, phone)
                            databaseHelper.addClassMember(classroomId, uid)
                            // Fetch profile image
                            fetchProfileImage(uid, name, isTeacher)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error checking teacher status: ${error.message}")
                            // Assume not teacher if error occurs
                            databaseHelper.updateUserProfile(uid, name, bio, phone)
                            databaseHelper.addClassMember(classroomId, uid)
                            fetchProfileImage(uid, name, false)
                        }
                    })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase user database error: ${error.message}")
                loadLocalPerson(uid)
            }
        })
    }

    private fun fetchProfileImage(uid: String, name: String, isTeacher: Boolean) {
        // Check if local image exists
        val localFile = File(filesDir, "${uid}_profile.png")
        if (localFile.exists()) {
            val person = Person(uid, name, localFile.absolutePath, isTeacher)
            if (!people.any { it.uid == uid }) {
                people.add(person)
                people.sortBy { it.name } // Sort alphabetically
                peopleAdapter.notifyDataSetChanged()
                Log.d(TAG, "Used existing local profile image for $uid: ${localFile.absolutePath}")
            }
            return
        }

        // Fetch from server
        val request = FetchImageRequest(uid)
        apiService.fetchProfileImage(request).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                var localImagePath: String? = null
                if (response.isSuccessful && response.body()?.success == true) {
                    val imageUrl = response.body()?.image_url
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d(TAG, "Fetched image URL for $uid: $imageUrl")
                        // Use Picasso to load and save the image
                        Picasso.get().load(imageUrl).into(object : Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    localImagePath = saveImageLocally(bitmap, uid)
                                    Log.d(TAG, "Image loaded and saved for $uid: $localImagePath")
                                    // Update SQLite
                                    if (localImagePath != null) {
                                        databaseHelper.updateUserProfileImage(uid, localImagePath)
                                    }
                                    // Add to people list
                                    val person = Person(uid, name, localImagePath, isTeacher)
                                    if (!people.any { it.uid == uid }) {
                                        people.add(person)
                                        people.sortBy { it.name } // Sort alphabetically
                                        peopleAdapter.notifyDataSetChanged()
                                        Log.d(TAG, "Added person $uid with localImagePath: $localImagePath")
                                    }
                                } else {
                                    Log.w(TAG, "Bitmap is null for $uid")
                                    addPersonWithNoImage(uid, name, isTeacher)
                                }
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.e(TAG, "Picasso failed to load image for $uid: ${e?.message}", e)
                                addPersonWithNoImage(uid, name, isTeacher)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                                // Optional: Log or handle placeholder loading
                            }
                        })
                    } else {
                        Log.w(TAG, "No image URL provided for $uid")
                        addPersonWithNoImage(uid, name, isTeacher)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed for $uid: ${response.code()} ${response.message()}")
                    addPersonWithNoImage(uid, name, isTeacher)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image for $uid: ${t.message}", t)
                addPersonWithNoImage(uid, name, isTeacher)
            }
        })
    }

    private fun addPersonWithNoImage(uid: String, name: String, isTeacher: Boolean) {
        val person = Person(uid, name, null, isTeacher)
        if (!people.any { it.uid == uid }) {
            people.add(person)
            people.sortBy { it.name } // Sort alphabetically
            peopleAdapter.notifyDataSetChanged()
            Log.d(TAG, "Added person $uid with no image")
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

    private fun loadLocalPeople() {
        val localUsers = databaseHelper.getUsersByClassroom(classroomId)
        for (userData in localUsers) {
            val uid = userData["uid"] ?: continue
            val name = userData["name"] ?: "User"
            val imagePath = userData["profile_image_path"]
            // Determine if user is the teacher
            val classroomData = databaseHelper.getClassroom(classroomId)
            val isTeacher = classroomData?.get("uid") == uid
            val person = Person(uid, name, imagePath, isTeacher)
            if (!people.any { it.uid == uid }) {
                people.add(person)
            }
        }
        people.sortBy { it.name } // Sort alphabetically
        peopleAdapter.notifyDataSetChanged()
        if (people.isEmpty()) {
            Log.d(TAG, "No local people found for classroom $classroomId")
        }
    }

    private fun loadLocalPerson(uid: String) {
        val userData = databaseHelper.getUserProfile(uid)
        if (userData != null) {
            val name = userData["name"] ?: "User"
            val imagePath = userData["profile_image_path"]
            // Determine if user is the teacher
            val classroomData = databaseHelper.getClassroom(classroomId)
            val isTeacher = classroomData?.get("uid") == uid
            val person = Person(uid, name, imagePath, isTeacher)
            if (!people.any { it.uid == uid }) {
                people.add(person)
                people.sortBy { it.name } // Sort alphabetically
                peopleAdapter.notifyDataSetChanged()
                Log.d(TAG, "Loaded local person $uid with image path: $imagePath")
            }
        } else {
            Log.w(TAG, "No local user profile found for uid $uid")
        }
    }

    private fun filterPeople(query: String) {
        val filteredPeople = if (query.isEmpty()) {
            people.toList()
        } else {
            people.filter { it.name.contains(query, ignoreCase = true) }
        }
        peopleAdapter.updateList(filteredPeople)
    }
}
