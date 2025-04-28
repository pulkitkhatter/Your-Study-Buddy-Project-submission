package com.example.adminapp

import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class MentorFormActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val SMS_PERMISSION_REQUEST_CODE = 101
    private val PASSWORD_LENGTH = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mentor_form)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etMentorRegNo = findViewById<EditText>(R.id.etMentorRegNo)
        val etMentorName = findViewById<EditText>(R.id.etMentorName)
        val etMentorEmail = findViewById<EditText>(R.id.etMentorEmail)
        val etMentorPhone = findViewById<EditText>(R.id.etMentorPhone)
        val etMentorDepartment = findViewById<EditText>(R.id.etMentorDepartment)
        val btnSubmitMentor = findViewById<Button>(R.id.btnSubmitMentor)

        btnSubmitMentor.setOnClickListener {
            val regNo = etMentorRegNo.text.toString().trim()
            val name = etMentorName.text.toString().trim()
            val email = etMentorEmail.text.toString().trim()
            val phone = etMentorPhone.text.toString().trim()
            val department = etMentorDepartment.text.toString().trim()

            if (regNo.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty() || department.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            } else {
                if (checkSmsPermission()) {
                    registerMentor(regNo, name, email, phone, department)
                } else {
                    requestSmsPermission()
                }
            }
        }
    }

    private fun registerMentor(regNo: String, name: String, email: String, phone: String, department: String) {
        // Generate random password
        val password = generateRandomPassword(PASSWORD_LENGTH)

        // Create Firebase Auth user
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Update user profile with display name
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            // Save additional mentor data to Firestore
                            saveMentorToFirestore(regNo, name, email, phone, department, password)
                        } else {
                            Toast.makeText(this, "Failed to set user profile", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Authentication failed: ${authTask.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun generateRandomPassword(length: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun saveMentorToFirestore(
        regNo: String,
        name: String,
        email: String,
        phone: String,
        department: String,
        password: String
    ) {
        val mentorsRef = db.collection("mentors").document(email)
        val usersRef = db.collection("users").document(email)

        val mentor = hashMapOf(
            "registration_number" to regNo,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "department" to department,
            "password" to password // Note: Storing passwords is generally not recommended
        )

        val user = hashMapOf(
            "email" to email,
            "user_type" to "mentor",
            "name" to name
        )

        val batch = db.batch()
        batch.set(mentorsRef, mentor)
        batch.set(usersRef, user)

        batch.commit()
            .addOnSuccessListener {
                // Send SMS with credentials
                sendRegistrationSms(phone, name, email, password)
                Toast.makeText(this, "Mentor Registered Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to register mentor: ${e.message}", Toast.LENGTH_LONG).show()
                // Rollback auth creation if Firestore fails
                auth.currentUser?.delete()
            }
    }

    private fun sendRegistrationSms(phoneNumber: String, mentorName: String, email: String, password: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            val message = """
                Dear $mentorName,
                You have been successfully registered as a mentor.
                Email: $email
                Password: $password
                Please change your password after first login.
            """.trimIndent()
            smsManager.sendTextMessage(phoneNumber, "9996734575", message, null, null)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                } else {
                    Toast.makeText(this, "SMS permission is required to send notifications", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}