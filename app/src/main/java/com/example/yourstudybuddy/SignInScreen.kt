package com.example.yourstudybuddy

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private val NOTIFICATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        // If user is already signed in, redirect to the correct screen
        val storedUserType = sharedPreferences.getString("user_type", null)
        if (auth.currentUser != null && storedUserType != null) {
            redirectToScreen(storedUserType)
        }

        val userIdInput = findViewById<EditText>(R.id.userIdInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val signInButton = findViewById<Button>(R.id.signInButton)
        val guestUserButton = findViewById<TextView>(R.id.guestUser)
        val forgotPasswordButton = findViewById<TextView>(R.id.forgotPassword)

        signInButton.setOnClickListener {
            val userId = userIdInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (userId.isEmpty() || password.isEmpty()) {
                showAlertDialog("Invalid Credentials", "Please enter both email and password.")
            } else {
                signInUser(userId, password)
            }
        }

        guestUserButton.setOnClickListener {
            startActivity(Intent(this, GuestUserScreen::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            // Simply navigate to ForgetPasswordScreen without any checks
            startActivity(Intent(this, ForgetPasswrordScreen::class.java))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotificationPermission()
        }
    }

    private fun signInUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkUserType(email)
                } else {
                    showAlertDialog("Login Failed", "Invalid email/password or user does not exist.")
                }
            }
            .addOnFailureListener { exception ->
                showAlertDialog("Login Failed", exception.message ?: "Unknown error occurred.")
            }
    }

    private fun checkUserType(email: String) {
        val db = FirebaseFirestore.getInstance()
        val usersRef = db.collection("users").document(email)

        usersRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val userType = document.getString("user_type")
                sharedPreferences.edit().putString("user_type", userType).apply()
                redirectToScreen(userType)
            } else {
                showAlertDialog("Access Denied", "Your account is not registered.")
            }
        }.addOnFailureListener {
            showAlertDialog("Error", "Failed to fetch user details. Please try again.")
        }
    }

    private fun redirectToScreen(userType: String?) {
        val intent = when (userType) {
            "mentor" -> Intent(this, MentorScreen::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun askNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            showAlertDialog("Enable Notifications", "Please allow notifications to stay updated.") {
                requestNotificationPermission()
            }
        } else {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showAlertDialog("Notification Enabled", "You will receive important updates.")
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    showAlertDialog("Permission Denied", "Notifications are disabled. Enable them in settings.") {
                        openAppSettings()
                    }
                }
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun showAlertDialog(title: String, message: String, onPositiveClick: (() -> Unit)? = null) {
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onPositiveClick?.invoke()
            }

        if (onPositiveClick != null) {
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        }

        builder.show()
    }
}