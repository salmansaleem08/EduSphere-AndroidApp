package com.salmansaleem.edusphere

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")

        val nameField = findViewById<EditText>(R.id.et_full_name)
        val emailField = findViewById<EditText>(R.id.et_email)
        val phoneField = findViewById<EditText>(R.id.et_phone)
        val passwordField = findViewById<EditText>(R.id.et_password)
        val termsCheckbox = findViewById<CheckBox>(R.id.cb_terms)
        val signUpButton = findViewById<Button>(R.id.btn_sign_up)
        val loginText = findViewById<TextView>(R.id.tv_login)

        signUpButton.setOnClickListener {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (validateInputs(name, email, phone, password, termsCheckbox.isChecked)) {
                checkEmailAvailability(name, email, phone, password)
            }
        }

        var isPasswordVisible = false


        val toggleImage = findViewById<ImageView>(R.id.iv_toggle_password)

        toggleImage.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleImage.setImageResource(R.drawable.show)
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleImage.setImageResource(R.drawable.hide)
            }
            // move cursor to the end
            passwordField.setSelection(passwordField.text.length)
        }






//        loginText.setOnClickListener {
//            val intent = Intent(this, Login::class.java)
//            startActivity(intent)
//        }
    }

    private fun validateInputs(name: String, email: String, phone: String, password: String, termsAccepted: Boolean): Boolean {
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.length != 11 || !phone.all { it.isDigit() }) {
            Toast.makeText(this, "Phone number must be 11 digits", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!termsAccepted) {
            Toast.makeText(this, "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun checkEmailAvailability(name: String, email: String, phone: String, password: String) {
        database.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(this@SignUp, "Email already taken, choose another", Toast.LENGTH_SHORT).show()
                } else {
                    registerUser(name, email, phone, password)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignUp, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun registerUser(name: String, email: String, phone: String, password: String) {
        val signUpButton = findViewById<Button>(R.id.btn_sign_up)
        signUpButton.isEnabled = false // Disable button during registration

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: return@addOnCompleteListener
                    user.getIdToken(true).addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val idToken = tokenTask.result?.token
                            val userData = mapOf(
                                "uid" to uid,
                                "fullName" to name,
                                "email" to email,
                                "phone" to phone
                            )

                            database.child(uid).setValue(userData)
                                .addOnCompleteListener { dbTask ->
                                    signUpButton.isEnabled = true // Re-enable button
                                    if (dbTask.isSuccessful) {
                                        sendTokenToBackend(idToken, userData)
                                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                        //startActivity(Intent(this, MyProfile::class.java))
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Database error: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show()
                            signUpButton.isEnabled = true
                        }
                    }
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." -> "This email is already registered"
                        "The email address is badly formatted." -> "Invalid email format"
                        else -> "Registration failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    signUpButton.isEnabled = true
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
                    Toast.makeText(this@SignUp, "Backend error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@SignUp, "Backend verification failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}