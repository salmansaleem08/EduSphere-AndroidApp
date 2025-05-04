package com.salmansaleem.edusphere

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase and SQLite
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("EduSpherePrefs", MODE_PRIVATE)
        databaseHelper = DatabaseHelper(this)

        val emailField = findViewById<EditText>(R.id.et_email)
        val passwordField = findViewById<EditText>(R.id.et_password)
        val togglePassword = findViewById<ImageView>(R.id.iv_toggle_password)
        val rememberMeCheckbox = findViewById<CheckBox>(R.id.cb_remember_me)
        val forgotPasswordText = findViewById<TextView>(R.id.tv_forgot_password)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val signupText = findViewById<TextView>(R.id.tv_signup)

        // Restore "Remember me" state
        if (sharedPreferences.getBoolean("remember_me", false)) {
            emailField.setText(sharedPreferences.getString("email", ""))
            rememberMeCheckbox.isChecked = true
        }

        // Password visibility toggle
        togglePassword.setOnClickListener {
            if (passwordField.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePassword.setImageResource(R.drawable.show) // Replace with your show icon
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePassword.setImageResource(R.drawable.hide) // Replace with your hide icon
            }
            passwordField.setSelection(passwordField.text.length) // Keep cursor at end
        }

        // Login button
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password, rememberMeCheckbox.isChecked)
            }
        }

        // Forgot password (disabled offline)
        forgotPasswordText.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(this, "Password reset requires internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val email = emailField.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Sign-up navigation
        signupText.setOnClickListener {
            startActivity(Intent(this, SignUp::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun loginUser(email: String, password: String, rememberMe: Boolean) {
        val loginButton = findViewById<Button>(R.id.btn_login)
        loginButton.isEnabled = false

        if (isOnline()) {
            // Online: Use Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token
                                val userData = mapOf(
                                    "uid" to user.uid,
                                    "email" to email
                                )

                                // Save "Remember me" preference
                                with(sharedPreferences.edit()) {
                                    putBoolean("remember_me", rememberMe)
                                    putString("email", if (rememberMe) email else "")
                                    apply()
                                }

                                // Fetch name and phone from Firebase Realtime Database
                                val usersRef = FirebaseDatabase.getInstance().getReference("Users").child(user.uid)
                                usersRef.get().addOnCompleteListener { dbTask ->
                                    if (dbTask.isSuccessful) {
                                        val snapshot = dbTask.result
                                        val name = snapshot.child("fullName").getValue(String::class.java) ?: ""
                                        val phone = snapshot.child("phone").getValue(String::class.java) ?: ""

                                        // Store user data in SQLite
                                        val stored = databaseHelper.insertUser(name, email, phone, password)
                                        if (!stored) {
                                            Toast.makeText(this, "Failed to store user data locally", Toast.LENGTH_SHORT).show()
                                        }

                                        // Send token to backend
                                        sendTokenToBackend(idToken, userData)

                                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, Home::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Database error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMessage = when (task.exception?.message) {
                            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                                "No account found with this email"
                            "The password is invalid or the user does not have a password." ->
                                "Incorrect password"
                            else -> "Login failed: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            // Offline: Use SQLite Authentication
            val user = databaseHelper.verifyUser(email, password)
            loginButton.isEnabled = true
            if (user != null) {
                // Save "Remember me" preference
                with(sharedPreferences.edit()) {
                    putBoolean("remember_me", rememberMe)
                    putString("email", if (rememberMe) email else "")
                    apply()
                }

                Toast.makeText(this, "Offline Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Home::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendTokenToBackend(idToken: String?, userData: Map<String, String>) {
        val client = OkHttpClient()
        val json = """
            {
                "token": "$idToken",
                "userData": $userData
            }
        """.trimIndent()

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json
        )

        val request = Request.Builder()
            .url("https://your-backend-api.com/verify-token") // Replace with your backend endpoint
            .post(body)
            .addHeader("Authorization", "Bearer $idToken")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Login, "Backend error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@Login, "Backend verification failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}