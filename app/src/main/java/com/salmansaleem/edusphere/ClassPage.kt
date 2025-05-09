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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_page)

        // Initialize Firebase and SQLite
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Announcements")
        databaseHelper = DatabaseHelper(this)

        // Get classroom ID and name from intent
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        val classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"
        uid = auth.currentUser?.uid ?: ""

        // Initialize UI
        val titleTextView = findViewById<TextView>(R.id.tv_title)
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

        // Setup RecyclerView
        announcementAdapter = AnnouncementAdapter(announcements)
        announcementRecyclerView.layoutManager = LinearLayoutManager(this)
        announcementRecyclerView.adapter = announcementAdapter

        // Load announcements
        loadAnnouncements()

        // Send announcement
        sendButton.setOnClickListener {
            val text = announceEditText.text.toString().trim()
            if (text.isNotEmpty()) {
                postAnnouncement(text)
                announceEditText.text.clear()
            } else {
                Toast.makeText(this, "Please enter an announcement", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun postAnnouncement(text: String) {
        val announcementId = UUID.randomUUID().toString()
        val timestamp = SimpleDateFormat("hh:mm a · dd MMM yy", Locale.getDefault()).format(Date())
        if (isOnline()) {
            val announcementData = mapOf(
                "uid" to uid,
                "name" to userName,
                "text" to text,
                "timestamp" to timestamp
            )
            database.child(classroomId).child(announcementId).setValue(announcementData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        databaseHelper.insertAnnouncement(announcementId, classroomId, uid, userName, text, timestamp)
                        Toast.makeText(this, "Announcement posted", Toast.LENGTH_SHORT).show()
                        loadAnnouncements()
                    } else {
                        Log.e(TAG, "Firebase announcement error: ${task.exception?.message}")
                        databaseHelper.queueAnnouncementUpdate(announcementId, classroomId, uid, userName, text, timestamp)
                        databaseHelper.insertAnnouncement(announcementId, classroomId, uid, userName, text, timestamp)
                        Toast.makeText(this, "Announcement saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                        loadAnnouncements()
                    }
                }
        } else {
            databaseHelper.queueAnnouncementUpdate(announcementId, classroomId, uid, userName, text, timestamp)
            databaseHelper.insertAnnouncement(announcementId, classroomId, uid, userName, text, timestamp)
            Toast.makeText(this, "Announcement saved locally, will sync when online", Toast.LENGTH_SHORT).show()
            loadAnnouncements()
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
                        val announcement = Announcement(announcementId, classroomId, uid, name, text, timestamp)
                        if (!announcements.any { it.announcementId == announcementId }) {
                            announcements.add(announcement)
                        }
                    }
                    // Sort announcements by timestamp (newest first)
                    announcements.sortByDescending { it.timestamp }
                    announcementAdapter.notifyDataSetChanged()
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
            val announcement = Announcement(
                announcementId,
                announcementData["classroom_id"] ?: "",
                announcementData["uid"] ?: "",
                announcementData["name"] ?: "",
                announcementData["announcement_text"] ?: "",
                announcementData["timestamp"] ?: ""
            )
            if (!announcements.any { it.announcementId == announcementId }) {
                announcements.add(announcement)
            }
        }
        // Sort announcements by timestamp (newest first)
        announcements.sortByDescending { it.timestamp }
        announcementAdapter.notifyDataSetChanged()
    }
}