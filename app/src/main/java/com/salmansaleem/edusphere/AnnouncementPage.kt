package com.salmansaleem.edusphere

import android.content.Context
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
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AnnouncementPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var commentRecyclerView: RecyclerView
    private val comments = mutableListOf<Comment>()
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var announcementId: String
    private lateinit var classroomId: String
    private lateinit var classroomName: String
    private lateinit var uid: String
    private lateinit var userName: String
    private val TAG = "AnnouncementPage"


    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement_page)

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


        val profileImageView = findViewById<ImageView>(R.id.img_profile)




        val announcementUid = intent.getStringExtra("uid") ?: ""
        // Get data from intent
        announcementId = intent.getStringExtra("announcement_id") ?: ""
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"
        val teacherName = intent.getStringExtra("teacher_name") ?: "Teacher"
        val announcementText = intent.getStringExtra("announcement_text") ?: ""
        val timestamp = intent.getStringExtra("timestamp") ?: ""
        uid = auth.currentUser?.uid ?: ""


        fetchAnnouncementProfileImage(announcementUid, teacherName, announcementText, timestamp, profileImageView)


        // Initialize UI
        val teacherNameTextView = findViewById<TextView>(R.id.txt_teacher_name)
        val classNameTextView = findViewById<TextView>(R.id.Classname)
        val postContentTextView = findViewById<TextView>(R.id.txt_post_content)
        val postTimeTextView = findViewById<TextView>(R.id.txt_post_time)
        val commentEditText = findViewById<EditText>(R.id.comment)
        val sendButton = findViewById<ImageView>(R.id.iv_send)
        commentRecyclerView = findViewById<RecyclerView>(R.id.tv_announcement)

        // Set announcement details
        teacherNameTextView.text = teacherName
        classNameTextView.text = classroomName
        postContentTextView.text = announcementText
        postTimeTextView.text = timestamp

        // Get user name
        databaseHelper.getUserProfile(uid)?.let { user ->
            userName = user["name"] ?: "User"
        } ?: run {
            userName = "User"
        }

        // Setup RecyclerView
        commentAdapter = CommentAdapter(comments)
        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentRecyclerView.adapter = commentAdapter

        // Load comments
        loadComments()

        // Post comment
        sendButton.setOnClickListener {
            val text = commentEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text)
                commentEditText.text.clear()
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
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

//    private fun postComment(text: String) {
//        val commentId = UUID.randomUUID().toString()
//        val timestamp = SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
//        if (isOnline()) {
//            val commentData = mapOf(
//                "uid" to uid,
//                "name" to userName,
//                "text" to text,
//                "timestamp" to timestamp
//            )
//            database.child(classroomId).child(announcementId).child("comments").child(commentId).setValue(commentData)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
//                        Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
//                        loadComments()
//                    } else {
//                        Log.e(TAG, "Firebase comment error: ${task.exception?.message}")
//                        databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
//                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
//                        Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
//                        loadComments()
//                    }
//                }
//        } else {
//            databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
//            databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
//            Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
//            loadComments()
//        }
//    }


    private fun postComment(text: String) {
        val commentId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
        val profileImagePath = databaseHelper.getUserProfile(uid)?.get("profile_image_path")
        if (isOnline()) {
            val commentData = mapOf(
                "uid" to uid,
                "name" to userName,
                "text" to text,
                "timestamp" to timestamp
            )
            database.child(classroomId).child(announcementId).child("comments").child(commentId).setValue(commentData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
                        val comment = Comment(commentId, announcementId, classroomId, uid, userName, text, timestamp, profileImagePath)
                        comments.add(comment)
                        comments.sortByDescending { it.timestamp }
                        commentAdapter.notifyDataSetChanged()
                        Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Firebase comment error: ${task.exception?.message}")
                        databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
                        val comment = Comment(commentId, announcementId, classroomId, uid, userName, text, timestamp, profileImagePath)
                        comments.add(comment)
                        comments.sortByDescending { it.timestamp }
                        commentAdapter.notifyDataSetChanged()
                        Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
            databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
            val comment = Comment(commentId, announcementId, classroomId, uid, userName, text, timestamp, profileImagePath)
            comments.add(comment)
            comments.sortByDescending { it.timestamp }
            commentAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun loadComments() {
//        comments.clear()
//        if (isOnline()) {
//            database.child(classroomId).child(announcementId).child("comments").addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    var hasData = false
//                    for (commentSnapshot in snapshot.children) {
//                        hasData = true
//                        val commentId = commentSnapshot.key ?: continue
//                        val uid = commentSnapshot.child("uid").getValue(String::class.java) ?: ""
//                        val name = commentSnapshot.child("name").getValue(String::class.java) ?: ""
//                        val text = commentSnapshot.child("text").getValue(String::class.java) ?: ""
//                        val timestamp = commentSnapshot.child("timestamp").getValue(String::class.java) ?: ""
//                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, name, text, timestamp)
//                        val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp)
//                        if (!comments.any { it.commentId == commentId }) {
//                            comments.add(comment)
//                        }
//                    }
//                    // Sort comments by timestamp (newest first)
//                    comments.sortByDescending { it.timestamp }
//                    commentAdapter.notifyDataSetChanged()
//                    if (!hasData) {
//                        loadLocalComments()
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e(TAG, "Firebase database error: ${error.message}")
//                    loadLocalComments()
//                }
//            })
//        } else {
//            loadLocalComments()
//        }
//    }


    private fun loadComments() {
        comments.clear()
        if (isOnline()) {
            database.child(classroomId).child(announcementId).child("comments").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var hasData = false
                    for (commentSnapshot in snapshot.children) {
                        hasData = true
                        val commentId = commentSnapshot.key ?: continue
                        val uid = commentSnapshot.child("uid").getValue(String::class.java) ?: ""
                        val name = commentSnapshot.child("name").getValue(String::class.java) ?: ""
                        val text = commentSnapshot.child("text").getValue(String::class.java) ?: ""
                        val timestamp = commentSnapshot.child("timestamp").getValue(String::class.java) ?: ""
                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, name, text, timestamp)
                        fetchCommentProfileImage(uid, name, commentId, announcementId, classroomId, text, timestamp)
                    }
                    if (!hasData) {
                        loadLocalComments()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Firebase database error: ${error.message}")
                    loadLocalComments()
                }
            })
        } else {
            loadLocalComments()
        }
    }

//    private fun loadLocalComments() {
//        val localComments = databaseHelper.getComments(announcementId)
//        for (commentData in localComments) {
//            val commentId = commentData["comment_id"] ?: continue
//            val comment = Comment(
//                commentId,
//                commentData["announcement_id"] ?: "",
//                commentData["classroom_id"] ?: "",
//                commentData["uid"] ?: "",
//                commentData["name"] ?: "",
//                commentData["comment_text"] ?: "",
//                commentData["timestamp"] ?: ""
//            )
//            if (!comments.any { it.commentId == commentId }) {
//                comments.add(comment)
//            }
//        }
//        // Sort comments by timestamp (newest first)
//        comments.sortByDescending { it.timestamp }
//        commentAdapter.notifyDataSetChanged()
//    }


    private fun loadLocalComments() {
        val localComments = databaseHelper.getComments(announcementId)
        for (commentData in localComments) {
            val commentId = commentData["comment_id"] ?: continue
            val classroomId = commentData["classroom_id"] ?: ""
            val uid = commentData["uid"] ?: ""
            val name = commentData["name"] ?: ""
            val text = commentData["comment_text"] ?: ""
            val timestamp = commentData["timestamp"] ?: ""
            val profileImagePath = databaseHelper.getUserProfile(uid)?.get("profile_image_path")
            val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp, profileImagePath)
            if (!comments.any { it.commentId == commentId }) {
                comments.add(comment)
            }
        }
        comments.sortByDescending { it.timestamp }
        commentAdapter.notifyDataSetChanged()
    }

    private fun fetchAnnouncementProfileImage(uid: String, name: String, text: String, timestamp: String, profileImageView: ImageView) {
        val localFile = File(filesDir, "${uid}_profile.png")
        if (localFile.exists()) {
            Picasso.get()
                .load(localFile)
                .placeholder(R.drawable.user_profile_placeholder)
                .error(R.drawable.user_profile_placeholder)
                .into(profileImageView)
            Log.d(TAG, "Used existing local profile image for announcement $uid: ${localFile.absolutePath}")
            return
        }

        val request = FetchImageRequest(uid)
        apiService.fetchProfileImage(request).enqueue(object : Callback<ProfileImageResponse> {
            override fun onResponse(call: Call<ProfileImageResponse>, response: Response<ProfileImageResponse>) {
                var localImagePath: String? = null
                if (response.isSuccessful && response.body()?.success == true) {
                    val imageUrl = response.body()?.image_url
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d(TAG, "Fetched image URL for announcement $uid: $imageUrl")
                        Picasso.get().load(imageUrl).into(object : Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    localImagePath = saveImageLocally(bitmap, uid)
                                    Log.d(TAG, "Image loaded and saved for announcement $uid: $localImagePath")
                                    if (localImagePath != null) {
                                        databaseHelper.updateUserProfileImage(uid, localImagePath)
                                        Picasso.get()
                                            .load(File(localImagePath))
                                            .placeholder(R.drawable.user_profile_placeholder)
                                            .error(R.drawable.user_profile_placeholder)
                                            .into(profileImageView)
                                    }
                                } else {
                                    Log.w(TAG, "Bitmap is null for announcement $uid")
                                    loadPlaceholderImage(profileImageView)
                                }
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.e(TAG, "Picasso failed to load image for announcement $uid: ${e?.message}", e)
                                loadPlaceholderImage(profileImageView)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                        })
                    } else {
                        Log.w(TAG, "No image URL provided for announcement $uid")
                        loadPlaceholderImage(profileImageView)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed for announcement $uid: ${response.code()} ${response.message()}")
                    loadPlaceholderImage(profileImageView)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image for announcement $uid: ${t.message}", t)
                loadPlaceholderImage(profileImageView)
            }
        })
    }

    private fun fetchCommentProfileImage(uid: String, name: String, commentId: String, announcementId: String, classroomId: String, text: String, timestamp: String) {
        val localFile = File(filesDir, "${uid}_profile.png")
        if (localFile.exists()) {
            val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp, localFile.absolutePath)
            if (!comments.any { it.commentId == commentId }) {
                comments.add(comment)
                comments.sortByDescending { it.timestamp }
                commentAdapter.notifyDataSetChanged()
                Log.d(TAG, "Used existing local profile image for comment $uid: ${localFile.absolutePath}")
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
                        Log.d(TAG, "Fetched image URL for comment $uid: $imageUrl")
                        Picasso.get().load(imageUrl).into(object : Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                if (bitmap != null) {
                                    localImagePath = saveImageLocally(bitmap, uid)
                                    Log.d(TAG, "Image loaded and saved for comment $uid: $localImagePath")
                                    if (localImagePath != null) {
                                        databaseHelper.updateUserProfileImage(uid, localImagePath)
                                    }
                                    val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp, localImagePath)
                                    if (!comments.any { it.commentId == commentId }) {
                                        comments.add(comment)
                                        comments.sortByDescending { it.timestamp }
                                        commentAdapter.notifyDataSetChanged()
                                        Log.d(TAG, "Added comment $commentId with localImagePath: $localImagePath")
                                    }
                                } else {
                                    Log.w(TAG, "Bitmap is null for comment $uid")
                                    addCommentWithNoImage(commentId, announcementId, classroomId, uid, name, text, timestamp)
                                }
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.e(TAG, "Picasso failed to load image for comment $uid: ${e?.message}", e)
                                addCommentWithNoImage(commentId, announcementId, classroomId, uid, name, text, timestamp)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                        })
                    } else {
                        Log.w(TAG, "No image URL provided for comment $uid")
                        addCommentWithNoImage(commentId, announcementId, classroomId, uid, name, text, timestamp)
                    }
                } else {
                    Log.e(TAG, "Fetch profile image failed for comment $uid: ${response.code()} ${response.message()}")
                    addCommentWithNoImage(commentId, announcementId, classroomId, uid, name, text, timestamp)
                }
            }

            override fun onFailure(call: Call<ProfileImageResponse>, t: Throwable) {
                Log.e(TAG, "Network error fetching profile image for comment $uid: ${t.message}", t)
                addCommentWithNoImage(commentId, announcementId, classroomId, uid, name, text, timestamp)
            }
        })
    }

    private fun addCommentWithNoImage(commentId: String, announcementId: String, classroomId: String, uid: String, name: String, text: String, timestamp: String) {
        val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp, null)
        if (!comments.any { it.commentId == commentId }) {
            comments.add(comment)
            comments.sortByDescending { it.timestamp }
            commentAdapter.notifyDataSetChanged()
            Log.d(TAG, "Added comment $commentId with no image")
        }
    }

    private fun loadPlaceholderImage(imageView: ImageView) {
        Picasso.get()
            .load(R.drawable.user_profile_placeholder)
            .into(imageView)
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
}
