package com.salmansaleem.edusphere

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class AssignmentSubmission : AppCompatActivity() {
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var apiService: ApiService
    private lateinit var classroomId: String
    private lateinit var assignmentId: String
    private lateinit var userId: String
    private var submissionImageUri: Uri? = null
    private var submissionImagePath: String? = null
    private val TAG = "AssignmentSubmission"
    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment_submission)

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

        // Get classroom and assignment IDs from intent
        classroomId = intent.getStringExtra("classroom_id") ?: ""
        assignmentId = intent.getStringExtra("assignment_id") ?: ""

        // Get user ID
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e(TAG, "User not authenticated")
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        userId = user.uid

        // Initialize UI
        val backButton = findViewById<ImageView>(R.id.iv_back)
        val titleTextView = findViewById<TextView>(R.id.tv_title)
        val fromValueTextView = findViewById<TextView>(R.id.tv_from_value)
        val dueValueTextView = findViewById<TextView>(R.id.tv_due_value)
        val descriptionValueTextView = findViewById<TextView>(R.id.tv_description_value)
        val assignmentImageContainer = findViewById<LinearLayout>(R.id.assignment_image_container)
        val assignmentImageNameTextView = findViewById<TextView>(R.id.tv_assignment_image_name)
        val attachedFilesRecyclerView = findViewById<RecyclerView>(R.id.rv_attached_files)
        val addMoreFilesButton = findViewById<RelativeLayout>(R.id.btn_add_more_files)
        val submitButton = findViewById<Button>(R.id.btn_submit)


        // Inside onCreate, after UI initialization
        scheduleSyncWorker()

        // Initialize RecyclerView for attached files
        val attachedFilesAdapter = AttachmentsAdapter(mutableListOf()) { filePath ->
            showFullImage(filePath)
        }
        attachedFilesRecyclerView.layoutManager = LinearLayoutManager(this)
        attachedFilesRecyclerView.adapter = attachedFilesAdapter

        // Back button
        backButton.setOnClickListener { finish() }

        // Load assignment details
        loadAssignmentDetails(titleTextView, fromValueTextView, dueValueTextView, descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView)

        // Load submission (if exists)
        loadSubmission(attachedFilesAdapter)

        // Add more files
        val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                submissionImageUri = uri
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "submission_image.png"
                try {
                    val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                    val file = File(filesDir, "${assignmentId}_${userId}_submission.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    submissionImagePath = file.absolutePath
                    attachedFilesAdapter.updateAttachments(listOf(mapOf("name" to fileName, "path" to submissionImagePath!!)))
                    Toast.makeText(this, "Image selected: $fileName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving submission image: ${e.message}")
                    Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                }
            }
        }
        addMoreFilesButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Submit button
        submitButton.setOnClickListener {
            if (submissionImagePath == null) {
                Toast.makeText(this, "Please upload an image to submit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitAssignment()
        }
    }

//    private fun loadAssignmentDetails(
//        titleTextView: TextView,
//        fromValueTextView: TextView,
//        dueValueTextView: TextView,
//        descriptionValueTextView: TextView,
//        assignmentImageContainer: LinearLayout,
//        assignmentImageNameTextView: TextView
//    ) {
//        if (isOnline()) {
//            // Fetch from Firebase
//            FirebaseDatabase.getInstance().getReference("Assignments").child(classroomId).child(assignmentId)
//                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
//                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
//                        val assignment = snapshot.value as? Map<String, Any>
//                        if (assignment != null) {
//                            titleTextView.text = assignment["name"]?.toString() ?: ""
//                            fromValueTextView.text = assignment["uid"]?.toString() ?: "Unknown"
//                            val dueDate = assignment["due_date"]?.toString() ?: ""
//                            dueValueTextView.text = try {
//                                val parsedDate = dateFormat.parse(dueDate)
//                                displayDateFormat.format(parsedDate!!)
//                            } catch (e: Exception) {
//                                dueDate
//                            }
//                            descriptionValueTextView.text = assignment["description"]?.toString() ?: ""
//                            fetchAssignmentImage(assignmentImageContainer, assignmentImageNameTextView)
//                        } else {
//                            loadAssignmentFromLocal(titleTextView, fromValueTextView, dueValueTextView, descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView)
//                        }
//                    }
//                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
//                        Log.e(TAG, "Firebase assignment fetch error: ${error.message}")
//                        loadAssignmentFromLocal(titleTextView, fromValueTextView, dueValueTextView, descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView)
//                    }
//                })
//        } else {
//            loadAssignmentFromLocal(titleTextView, fromValueTextView, dueValueTextView, descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView)
//        }
//    }


    private fun loadAssignmentDetails(
        titleTextView: TextView,
        fromValueTextView: TextView,
        dueValueTextView: TextView,
        descriptionValueTextView: TextView,
        assignmentImageContainer: LinearLayout,
        assignmentImageNameTextView: TextView
    ) {
        if (isOnline()) {
            // Fetch from Firebase
            FirebaseDatabase.getInstance().getReference("Assignments").child(classroomId).child(assignmentId)
                .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                    override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                        val assignment = snapshot.value as? Map<String, Any>
                        if (assignment != null) {
                            titleTextView.text = assignment["name"]?.toString() ?: ""
                            val teacherUid = assignment["uid"]?.toString() ?: "Unknown"
                            val localTeacherName = databaseHelper.getTeacherNameByUid(teacherUid)
                            if (localTeacherName != null) {
                                fromValueTextView.text = localTeacherName
                            } else {
                                // Fetch from Firebase if not found locally
                                fetchTeacherNameFromFirebase(teacherUid) { name ->
                                    fromValueTextView.text = name ?: "Unknown"
                                }
                            }
                            val dueDate = assignment["due_date"]?.toString() ?: ""
                            dueValueTextView.text = try {
                                val parsedDate = dateFormat.parse(dueDate)
                                displayDateFormat.format(parsedDate!!)
                            } catch (e: Exception) {
                                dueDate
                            }
                            descriptionValueTextView.text = assignment["description"]?.toString() ?: ""
                            // Always fetch the image from the server
                            fetchAssignmentImage(assignmentImageContainer, assignmentImageNameTextView)
                        } else {
                            loadAssignmentFromLocal(
                                titleTextView, fromValueTextView, dueValueTextView,
                                descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView
                            )
                        }
                    }
                    override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                        Log.e(TAG, "Firebase assignment fetch error: ${error.message}")
                        loadAssignmentFromLocal(
                            titleTextView, fromValueTextView, dueValueTextView,
                            descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView
                        )
                    }
                })
        } else {
            loadAssignmentFromLocal(
                titleTextView, fromValueTextView, dueValueTextView,
                descriptionValueTextView, assignmentImageContainer, assignmentImageNameTextView
            )
        }
    }

//    private fun loadAssignmentFromLocal(
//        titleTextView: TextView,
//        fromValueTextView: TextView,
//        dueValueTextView: TextView,
//        descriptionValueTextView: TextView,
//        assignmentImageContainer: LinearLayout,
//        assignmentImageNameTextView: TextView
//    ) {
//        val assignment = databaseHelper.getAssignment(assignmentId)
//        if (assignment != null) {
//            titleTextView.text = assignment["name"] ?: ""
//            fromValueTextView.text = assignment["uid"] ?: "Unknown"
//            val dueDate = assignment["due_date"] ?: ""
//            dueValueTextView.text = try {
//                val parsedDate = dateFormat.parse(dueDate)
//                displayDateFormat.format(parsedDate!!)
//            } catch (e: Exception) {
//                dueDate
//            }
//            descriptionValueTextView.text = assignment["description"] ?: ""
//            val imagePath = assignment["image_path"]
//            if (!imagePath.isNullOrEmpty()) {
//                val fileName = File(imagePath).name
//                assignmentImageContainer.visibility = View.VISIBLE
//                assignmentImageNameTextView.text = fileName
//                assignmentImageNameTextView.setOnClickListener { showFullImage(imagePath) }
//            } else {
//                assignmentImageContainer.visibility = View.GONE
//            }
//        } else {
//            Toast.makeText(this, "Assignment not found", Toast.LENGTH_SHORT).show()
//            finish()
//        }
//    }


    private fun loadAssignmentFromLocal(
        titleTextView: TextView,
        fromValueTextView: TextView,
        dueValueTextView: TextView,
        descriptionValueTextView: TextView,
        assignmentImageContainer: LinearLayout,
        assignmentImageNameTextView: TextView
    ) {
        val assignment = databaseHelper.getAssignment(assignmentId)
        if (assignment != null) {
            titleTextView.text = assignment["name"] ?: ""
            fromValueTextView.text = assignment["teacher_name"] ?: "Unknown"
            val dueDate = assignment["due_date"] ?: ""
            dueValueTextView.text = try {
                val parsedDate = dateFormat.parse(dueDate)
                displayDateFormat.format(parsedDate!!)
            } catch (e: Exception) {
                dueDate
            }
            descriptionValueTextView.text = assignment["description"] ?: ""
            val imagePath = assignment["image_path"]
            if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                val fileName = File(imagePath).name
                assignmentImageContainer.visibility = View.VISIBLE
                assignmentImageNameTextView.text = fileName
                assignmentImageNameTextView.setOnClickListener { showFullImage(imagePath) }
            } else {
                // If image is not available locally, try to fetch it if online
                if (isOnline()) {
                    fetchAssignmentImage(assignmentImageContainer, assignmentImageNameTextView)
                } else {
                    assignmentImageContainer.visibility = View.GONE
                }
            }
        } else {
            Toast.makeText(this, "Assignment not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun fetchAssignmentImage(assignmentImageContainer: LinearLayout, assignmentImageNameTextView: TextView) {
        apiService.fetchAssignmentImage(FetchAssignmentImageRequest(assignmentId)).enqueue(object : Callback<AssignmentImageResponse> {
            override fun onResponse(call: Call<AssignmentImageResponse>, response: Response<AssignmentImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val imageUrl = response.body()?.image_url
                    if (!imageUrl.isNullOrEmpty()) {
                        // Launch a coroutine to handle the image download in the background
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // Download the image on a background thread
                                val bitmap = downloadImage(imageUrl)
                                if (bitmap != null) {
                                    val fileName = imageUrl.substringAfterLast("/")
                                    val file = File(filesDir, fileName)
                                    if (file.createNewFile() || file.exists()) {
                                        FileOutputStream(file).use { out ->
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        // Switch to the main thread to update the UI
                                        withContext(Dispatchers.Main) {
                                            val assignment = databaseHelper.getAssignment(assignmentId)
                                            if (assignment != null) {
                                                val uid = assignment["uid"] ?: ""
                                                val name = assignment["name"] ?: ""
                                                val description = assignment["description"] ?: ""
                                                val dueDate = assignment["due_date"] ?: ""
                                                val score = assignment["score"]?.toIntOrNull() ?: 0
                                                databaseHelper.insertAssignment(
                                                    assignmentId, classroomId, uid, name, description, dueDate, score, file.absolutePath
                                                )
                                                assignmentImageContainer.visibility = View.VISIBLE
                                                assignmentImageNameTextView.text = fileName
                                                assignmentImageNameTextView.setOnClickListener { showFullImage(file.absolutePath) }
                                            } else {
                                                Log.e(TAG, "Assignment not found for ID: $assignmentId")
                                                assignmentImageContainer.visibility = View.GONE
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Failed to create file: $fileName")
                                        withContext(Dispatchers.Main) {
                                            assignmentImageContainer.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "Failed to decode image from URL: $imageUrl")
                                    withContext(Dispatchers.Main) {
                                        assignmentImageContainer.visibility = View.GONE
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error saving assignment image: ${e.javaClass.simpleName} - ${e.message}", e)
                                withContext(Dispatchers.Main) {
                                    assignmentImageContainer.visibility = View.GONE
                                }
                            }
                        }
                    } else {
                        Log.e(TAG, "Image URL is null or empty")
                        assignmentImageContainer.visibility = View.GONE
                    }
                } else {
                    Log.e(TAG, "Image fetch failed: ${response.code()} ${response.message()}")
                    assignmentImageContainer.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<AssignmentImageResponse>, t: Throwable) {
                Log.e(TAG, "Image fetch error: ${t.message}", t)
                assignmentImageContainer.visibility = View.GONE
            }
        })
    }

    // Helper function to download the image in a background thread
    private suspend fun downloadImage(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image: ${e.message}", e)
                null
            }
        }
    }

    private fun loadSubmission(adapter: AttachmentsAdapter) {
        val submission = databaseHelper.getSubmission(assignmentId, userId)
        if (submission != null) {
            val imagePath = submission["submission_image_path"]
            if (!imagePath.isNullOrEmpty()) {
                val fileName = File(imagePath).name
                adapter.updateAttachments(listOf(mapOf("name" to fileName, "path" to imagePath)))
            }
            findViewById<Button>(R.id.btn_submit).isEnabled = false // Disable submit if already submitted
        }
    }

//    private fun submitAssignment() {
//        val submissionId = UUID.randomUUID().toString()
//        val submittedAt = dateFormat.format(Date())
//
//        // Save submission locally
//        val inserted = databaseHelper.insertSubmission(
//            submissionId, assignmentId, classroomId, userId, submittedAt, submissionImagePath
//        )
//        if (!inserted) {
//            Toast.makeText(this, "Failed to save submission locally", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        if (isOnline()) {
//            // Sync to Firebase
//            val submissionData = mapOf(
//                "submission_id" to submissionId,
//                "submitted_at" to submittedAt
//            )
//            FirebaseDatabase.getInstance().getReference("Submissions").child(classroomId).child(userId).child(assignmentId)
//                .setValue(submissionData)
//                .addOnSuccessListener {
//                    Log.d(TAG, "Submission synced to Firebase: $submissionId")
//                    uploadSubmissionImage(submissionId)
//                }
//                .addOnFailureListener { e ->
//                    Log.e(TAG, "Failed to sync submission to Firebase: ${e.message}")
//                    Toast.makeText(this, "Submission saved locally, will sync when online", Toast.LENGTH_SHORT).show()
//                    finish()
//                }
//        } else {
//            Toast.makeText(this, "Submission saved locally, will sync when online", Toast.LENGTH_SHORT).show()
//            finish()
//        }
//    }


    private fun submitAssignment() {
        val submissionId = UUID.randomUUID().toString()
        val submittedAt = dateFormat.format(Date())

        // Save submission locally
        val inserted = databaseHelper.insertSubmission(
            submissionId, assignmentId, classroomId, userId, submittedAt, submissionImagePath
        )
        if (!inserted) {
            Toast.makeText(this, "Failed to save submission locally", Toast.LENGTH_SHORT).show()
            return
        }

        // Queue for sync
        databaseHelper.queueSubmissionUpdate(
            submissionId, assignmentId, classroomId, userId, submittedAt, submissionImagePath
        )

        if (isOnline()) {
            // Sync to Firebase
            val submissionData = mapOf(
                "submission_id" to submissionId,
                "assignment_id" to assignmentId,
                "submitted_at" to submittedAt
            )
            FirebaseDatabase.getInstance().getReference("Submissions").child(classroomId).child(userId).child(assignmentId)
                .setValue(submissionData)
                .addOnSuccessListener {
                    Log.d(TAG, "Submission synced to Firebase: $submissionId")
                    uploadSubmissionImage(submissionId)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync submission to Firebase: ${e.message}")
                    Toast.makeText(this, "Submission saved locally, will sync when online", Toast.LENGTH_SHORT).show()
                    finish()
                }
        } else {
            Toast.makeText(this, "Submission saved locally, will sync when online", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


//    private fun uploadSubmissionImage(submissionId: String) {
//        val file = File(submissionImagePath ?: return)
//        if (!file.exists()) {
//            Log.e(TAG, "Submission image file does not exist: $submissionImagePath")
//            Toast.makeText(this, "Submission saved, image will sync later", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
//        val stream = java.io.ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        val byteArray = stream.toByteArray()
//        val imagePart = okhttp3.MultipartBody.Part.createFormData(
//            "image",
//            "${submissionId}_submission.png",
//            okhttp3.RequestBody.create("image/png".toMediaType(), byteArray)
//        )
//        val submissionIdPart = okhttp3.RequestBody.create("text/plain".toMediaType(), submissionId)
//        apiService.uploadSubmissionImage(submissionIdPart, imagePart).enqueue(object : Callback<AssignmentImageResponse> {
//            override fun onResponse(call: Call<AssignmentImageResponse>, response: Response<AssignmentImageResponse>) {
//                if (response.isSuccessful && response.body()?.success == true) {
//                    response.body()?.image_url?.let { url ->
//                        try {
//                            val bitmapFromServer = BitmapFactory.decodeStream(URL(url).openStream())
//                            val fileName = "${submissionId}_submission.png"
//                            val newFile = File(filesDir, fileName)
//                            FileOutputStream(newFile).use { out ->
//                                bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
//                            }
//                            Log.d(TAG, "Saved synced submission image locally at: ${newFile.absolutePath}")
//                            databaseHelper.insertSubmission(
//                                submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), newFile.absolutePath
//                            )
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Error saving synced submission image: ${e.message}")
//                        }
//                    }
//                    Toast.makeText(this@AssignmentSubmission, "Submission completed successfully", Toast.LENGTH_SHORT).show()
//                    finish()
//                } else {
//                    Log.e(TAG, "Submission image upload failed: ${response.code()} ${response.message()}")
//                    Toast.makeText(this@AssignmentSubmission, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
//                    finish()
//                }
//            }
//            override fun onFailure(call: Call<AssignmentImageResponse>, t: Throwable) {
//                Log.e(TAG, "Submission image upload error: ${t.message}")
//                Toast.makeText(this@AssignmentSubmission, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
//                finish()
//            }
//        })
//    }


    private fun uploadSubmissionImage(submissionId: String) {
        val file = File(submissionImagePath ?: return)
        if (!file.exists()) {
            Log.e(TAG, "Submission image file does not exist: $submissionImagePath")
            databaseHelper.queueSubmissionUpdate(
                submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), submissionImagePath
            )
            Toast.makeText(this, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            "${submissionId}_submission.png",
            RequestBody.create("image/png".toMediaType(), byteArray)
        )
        val submissionIdPart = RequestBody.create("text/plain".toMediaType(), submissionId)
        apiService.uploadSubmissionImage(submissionIdPart, imagePart).enqueue(object : Callback<SubmissionImageResponse> {
            override fun onResponse(call: Call<SubmissionImageResponse>, response: Response<SubmissionImageResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.image_url?.let { url ->
                        try {
                            val bitmapFromServer = BitmapFactory.decodeStream(URL(url).openStream())
                            val fileName = "${submissionId}_submission.png"
                            val newFile = File(filesDir, fileName)
                            FileOutputStream(newFile).use { out ->
                                bitmapFromServer.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            Log.d(TAG, "Saved synced submission image locally at: ${newFile.absolutePath}")
                            databaseHelper.insertSubmission(
                                submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), newFile.absolutePath
                            )
                            databaseHelper.deleteSubmissionUpdate(submissionId)
                            Toast.makeText(this@AssignmentSubmission, "Submission completed successfully", Toast.LENGTH_SHORT).show()
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving synced submission image: ${e.message}")
                            databaseHelper.queueSubmissionUpdate(
                                submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), submissionImagePath
                            )
                            Toast.makeText(this@AssignmentSubmission, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    Log.e(TAG, "Submission image upload failed: ${response.code()} ${response.body()?.error ?: response.message()}")
                    databaseHelper.queueSubmissionUpdate(
                        submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), submissionImagePath
                    )
                    Toast.makeText(this@AssignmentSubmission, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            override fun onFailure(call: Call<SubmissionImageResponse>, t: Throwable) {
                Log.e(TAG, "Submission image upload error: ${t.message}")
                databaseHelper.queueSubmissionUpdate(
                    submissionId, assignmentId, classroomId, userId, dateFormat.format(Date()), submissionImagePath
                )
                Toast.makeText(this@AssignmentSubmission, "Submission saved locally, image will sync later", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun showFullImage(filePath: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_full_image)
        val imageView = dialog.findViewById<ImageView>(R.id.iv_full_image)
        try {
            val bitmap = BitmapFactory.decodeFile(filePath)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading full image: ${e.message}")
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
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
            .enqueueUniquePeriodicWork("submission_sync", androidx.work.ExistingPeriodicWorkPolicy.KEEP, syncRequest)
    }


    private fun fetchTeacherNameFromFirebase(uid: String, callback: (String?) -> Unit) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.value as? Map<String, Any>
                    val name = user?.get("fullName")?.toString()
                    if (name != null) {
                        // Save to local database
                        databaseHelper.insertOrUpdateUser(
                            uid,
                            name,
                            user["email"]?.toString() ?: "",
                            user["phone"]?.toString() ?: "",
                            user["bio"]?.toString()
                        )
                        callback(name)
                    } else {
                        callback(null)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to fetch user from Firebase: ${error.message}")
                    callback(null)
                }
            })
    }
}