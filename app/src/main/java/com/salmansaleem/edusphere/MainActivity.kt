package com.salmansaleem.edusphere

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if this is a new installation or reinstall
            val isNewInstall = !sharedPreferences.getBoolean("hasBeenInstalled", false)

            // Set the flag to indicate the app has been installed
            sharedPreferences.edit().putBoolean("hasBeenInstalled", true).apply()

            // Check if user is logged in
            val currentUser = auth.currentUser
            val intent = if (currentUser == null || isNewInstall) {
                // First-time user, reinstalled user, or logged out - go to SignUp
                Intent(this, SignUp::class.java)
            } else {
                // Logged-in user with prior installation - go to Home
                Intent(this, SignUp::class.java)
            }

            startActivity(intent)
            finish()
        }, 2500)


        // Update FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseDatabase.getInstance().reference
                        .child("Users")
                        .child(userId)
                        .child("fcmToken")
                        .setValue(token)
                }
            }
        }
    }
}