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
import java.util.Random

class StudentFormActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val SMS_PERMISSION_REQUEST_CODE = 101
    private val PASSWORD_LENGTH = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_form)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etStudentRegNo = findViewById<EditText>(R.id.etStudentRegNo)
        val etStudentName = findViewById<EditText>(R.id.etStudentName)
        val etStudentEmail = findViewById<EditText>(R.id.etStudentEmail)
        val etStudentPhone = findViewById<EditText>(R.id.etStudentPhone)
        val etStudentDepartment = findViewById<EditText>(R.id.etStudentDepartment)
        val btnSubmitStudent = findViewById<Button>(R.id.btnSubmitStudent)

        btnSubmitStudent.setOnClickListener {
            val regNo = etStudentRegNo.text.toString().trim()
            val name = etStudentName.text.toString().trim()
            val email = etStudentEmail.text.toString().trim()
            val phone = etStudentPhone.text.toString().trim()
            val department = etStudentDepartment.text.toString().trim()

            if (regNo.isEmpty() || name.isEmpty() || email.isEmpty() || phone.isEmpty() || department.isEmpty()) {
                Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show()
            } else {
                if (checkSmsPermission()) {
                    registerStudent(regNo, name, email, phone, department)
                } else {
                    requestSmsPermission()
                }
            }
        }
    }

    private fun registerStudent(regNo: String, name: String, email: String, phone: String, department: String) {
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
                            // Save additional student data to Firestore
                            saveStudentToFirestore(regNo, name, email, phone, department, password)
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

    private fun saveStudentToFirestore(
        regNo: String,
        name: String,
        email: String,
        phone: String,
        department: String,
        password: String
    ) {
        val studentsRef = db.collection("students").document(email)
        val usersRef = db.collection("users").document(email)

        val student = hashMapOf(
            "registration_number" to regNo,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "department" to department,
            "password" to password // Note: Consider security implications
        )

        val user = hashMapOf(
            "email" to email,
            "user_type" to "student",
            "name" to name
        )

        val batch = db.batch()
        batch.set(studentsRef, student)
        batch.set(usersRef, user)

        batch.commit()
            .addOnSuccessListener {
                // Send SMS with credentials
                sendRegistrationSms(phone, name, email, password)
                Toast.makeText(this, "Student Registered Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to register student: ${e.message}", Toast.LENGTH_LONG).show()
                // Rollback auth creation if Firestore fails
                auth.currentUser?.delete()
            }
    }

    private fun sendRegistrationSms(phoneNumber: String, studentName: String, email: String, password: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            val message = """
                Dear $studentName,
                Welcome to Your Study Buddy!
                Email: $email
                Password: $password
                Please change your password after first login.
            """.trimIndent()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
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