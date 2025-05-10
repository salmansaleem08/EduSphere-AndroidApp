package com.salmansaleem.edusphere

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class ClassTasks : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var classroomId: String
    private lateinit var classroomName: String
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var filterAssigned: TextView
    private lateinit var filterMissing: TextView
    private lateinit var filterCompleted: TextView
    private var currentFilter = "Completed"
    private val TAG = "ClassTasks"
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_class_tasks)

        // Initialize DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Get classroom ID and name from intent
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        classroomName = intent.getStringExtra("classroom_name") ?: "Classroom"

        // Initialize UI
        val backButton = findViewById<ImageView>(R.id.iv_back)
        val titleTextView = findViewById<TextView>(R.id.tv_title)
        filterAssigned = findViewById(R.id.filter_assigned)
        filterMissing = findViewById(R.id.filter_missing)
        filterCompleted = findViewById(R.id.filter_completed)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_tasks)

        // Set title
        titleTextView.text = classroomName

        // Back button
        backButton.setOnClickListener { finish() }

        // Initialize RecyclerView
        tasksAdapter = TasksAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = tasksAdapter

        // Set filter listeners
        filterAssigned.setOnClickListener { setFilter("Assigned") }
        filterMissing.setOnClickListener { setFilter("Missing") }
        filterCompleted.setOnClickListener { setFilter("Completed") }

        // Load tasks
        loadTasks()
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        // Update filter UI
        filterAssigned.apply {
            setBackgroundResource(if (filter == "Assigned") R.drawable.filter_box_selected else R.drawable.filter_box_unselected)
            setTextColor(if (filter == "Assigned") 0xFF000000.toInt() else 0xFF999999.toInt())
        }
        filterMissing.apply {
            setBackgroundResource(if (filter == "Missing") R.drawable.filter_box_selected else R.drawable.filter_box_unselected)
            setTextColor(if (filter == "Missing") 0xFF000000.toInt() else 0xFF999999.toInt())
        }
        filterCompleted.apply {
            setBackgroundResource(if (filter == "Completed") R.drawable.filter_box_selected else R.drawable.filter_box_unselected)
            setTextColor(if (filter == "Completed") 0xFF000000.toInt() else 0xFF999999.toInt())
        }
        loadTasks()
    }

    private fun loadTasks() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "User not authenticated")
            tasksAdapter.updateTasks(emptyList())
            return
        }
        val userId = user.uid

        if (isOnline()) {
            // Fetch from Firebase
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
                                    "due_date" to (assignment["due_date"]?.toString() ?: ""),
                                    "submission_id" to "" // Will be updated below
                                )
                            )
                        }
                        // Fetch submissions to determine status
                        FirebaseDatabase.getInstance().getReference("Submissions").child(classroomId)
                            .child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(subSnapshot: DataSnapshot) {
                                    val updatedAssignments = assignments.map { assignment ->
                                        val assignmentId = assignment["assignment_id"]!!
                                        val submission = subSnapshot.child(assignmentId).value as? Map<String, Any>
                                        assignment.toMutableMap().apply {
                                            put("submission_id", submission?.get("submission_id")?.toString() ?: "")
                                            put("submitted_at", submission?.get("submitted_at")?.toString() ?: "")
                                        }
                                    }
                                    // Save to local database
                                    updatedAssignments.forEach { assignment ->
                                        databaseHelper.insertAssignment(
                                            assignment["assignment_id"]!!,
                                            assignment["classroom_id"]!!,
                                            assignment["uid"]!!,
                                            assignment["name"]!!,
                                            "", // description
                                            assignment["due_date"]!!,
                                            0,  // score
                                            null // image_path
                                        )
                                        if (assignment["submission_id"]?.isNotEmpty() == true) {
                                            databaseHelper.insertSubmission(
                                                assignment["submission_id"]!!,
                                                assignment["assignment_id"]!!,
                                                assignment["classroom_id"]!!,
                                                userId,
                                                assignment["submitted_at"]!!,
                                                null // submission_image_path
                                            )
                                        }
                                    }
                                    filterAndUpdateTasks(updatedAssignments, userId)
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "Firebase submissions fetch error: ${error.message}")
                                    filterAndUpdateTasks(assignments, userId)
                                }
                            })
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Firebase assignments fetch error: ${error.message}")
                        loadTasksFromLocal(userId)
                    }
                })
        } else {
            // Fetch from local storage
            loadTasksFromLocal(userId)
        }
    }

    private fun loadTasksFromLocal(userId: String) {
        val assignments = databaseHelper.getAssignmentsWithStatus(classroomId, userId)
        filterAndUpdateTasks(assignments, userId)
    }

//    private fun filterAndUpdateTasks(assignments: List<Map<String, String>>, userId: String) {
//        val filteredTasks = assignments.filter { assignment ->
//            val dueDateStr = assignment["due_date"] ?: return@filter false
//            val submissionId = assignment["submission_id"] ?: ""
//            val dueDate = try {
//                dateFormat.parse(dueDateStr)?.time ?: return@filter false
//            } catch (e: Exception) {
//                Log.e(TAG, "Invalid due date format: $dueDateStr")
//                return@filter false
//            }
//            val currentTime = System.currentTimeMillis()
//
//            when (currentFilter) {
//                "Assigned" -> submissionId.isEmpty() && dueDate > currentTime
//                "Missing" -> submissionId.isEmpty() && dueDate <= currentTime
//                "Completed" -> submissionId.isNotEmpty()
//                else -> true
//            }
//        }.map { assignment ->
//            mapOf(
//                "assignment_id" to assignment["assignment_id"]!!,
//                "name" to assignment["name"]!!,
//                "due_date" to try {
//                    val parsedDate = dateFormat.parse(assignment["due_date"]!!)
//                    displayDateFormat.format(parsedDate!!)
//                } catch (e: Exception) {
//                    assignment["due_date"]!!
//                }
//            )
//        }
//        tasksAdapter.updateTasks(filteredTasks)
//    }


    private fun filterAndUpdateTasks(assignments: List<Map<String, String>>, userId: String) {
        val filteredTasks = assignments.filter { assignment ->
            val dueDateStr = assignment["due_date"] ?: return@filter false
            val submissionId = assignment["submission_id"] ?: ""
            val dueDate = try {
                dateFormat.parse(dueDateStr)?.time ?: return@filter false
            } catch (e: Exception) {
                Log.e(TAG, "Invalid due date format: $dueDateStr")
                return@filter false
            }
            val currentTime = System.currentTimeMillis()

            when (currentFilter) {
                "Assigned" -> submissionId.isEmpty() && dueDate > currentTime
                "Missing" -> submissionId.isEmpty() && dueDate <= currentTime
                "Completed" -> submissionId.isNotEmpty() // Only assignments with a submission_id
                else -> true
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
        Log.d(TAG, "Filtered tasks for $currentFilter: $filteredTasks")
        tasksAdapter.updateTasks(filteredTasks)
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
}