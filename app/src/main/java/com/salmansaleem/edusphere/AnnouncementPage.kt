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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement_page)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Announcements")
        databaseHelper = DatabaseHelper(this)

        // Get data from intent
        announcementId = intent.getStringExtra("announcement_id") ?: ""
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"
        val teacherName = intent.getStringExtra("teacher_name") ?: "Teacher"
        val announcementText = intent.getStringExtra("announcement_text") ?: ""
        val timestamp = intent.getStringExtra("timestamp") ?: ""
        uid = auth.currentUser?.uid ?: ""

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

    private fun postComment(text: String) {
        val commentId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
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
                        Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
                        loadComments()
                    } else {
                        Log.e(TAG, "Firebase comment error: ${task.exception?.message}")
                        databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
                        databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
                        Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                        loadComments()
                    }
                }
        } else {
            databaseHelper.queueCommentUpdate(commentId, announcementId, classroomId, uid, userName, text, timestamp)
            databaseHelper.insertComment(commentId, announcementId, classroomId, uid, userName, text, timestamp)
            Toast.makeText(this, "Comment saved locally, will sync when online", Toast.LENGTH_SHORT).show()
            loadComments()
        }
    }

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
                        val comment = Comment(commentId, announcementId, classroomId, uid, name, text, timestamp)
                        if (!comments.any { it.commentId == commentId }) {
                            comments.add(comment)
                        }
                    }
                    // Sort comments by timestamp (newest first)
                    comments.sortByDescending { it.timestamp }
                    commentAdapter.notifyDataSetChanged()
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

    private fun loadLocalComments() {
        val localComments = databaseHelper.getComments(announcementId)
        for (commentData in localComments) {
            val commentId = commentData["comment_id"] ?: continue
            val comment = Comment(
                commentId,
                commentData["announcement_id"] ?: "",
                commentData["classroom_id"] ?: "",
                commentData["uid"] ?: "",
                commentData["name"] ?: "",
                commentData["comment_text"] ?: "",
                commentData["timestamp"] ?: ""
            )
            if (!comments.any { it.commentId == commentId }) {
                comments.add(comment)
            }
        }
        // Sort comments by timestamp (newest first)
        comments.sortByDescending { it.timestamp }
        commentAdapter.notifyDataSetChanged()
    }
}
