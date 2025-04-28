package com.example.yourstudybuddy

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ForgetPasswrordScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_passwrord_screen)

        auth = FirebaseAuth.getInstance()

        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val currentPasswordEditText = findViewById<TextInputEditText>(R.id.currentPasswordEditText)
        val newPasswordEditText = findViewById<TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val updateButton = findViewById<Button>(R.id.updateButton)

        updateButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val currentPassword = currentPasswordEditText.text.toString().trim()
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            when {
                email.isEmpty() -> {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
                currentPassword.isEmpty() -> {
                    Toast.makeText(this, "Enter current password", Toast.LENGTH_SHORT).show()
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "Enter new password", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 6 -> {
                    Toast.makeText(this, "Password too short (min 6 characters)", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    updatePassword(email, currentPassword, newPassword)
                }
            }
        }
    }

    private fun updatePassword(email: String, currentPassword: String, newPassword: String) {
        // First sign in with the provided credentials
        auth.signInWithEmailAndPassword(email, currentPassword)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    // Now update the password
                    val user = auth.currentUser
                    user?.updatePassword(newPassword)
                        ?.addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Authentication failed. Wrong email or password?", Toast.LENGTH_SHORT).show()
                }
            }
    }
}