package com.example.adminapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_screen)

        val tvMentorCount = findViewById<TextView>(R.id.tvMentorCount)
        val tvStudentCount = findViewById<TextView>(R.id.tvStudentCount)
        val btnViewMentors = findViewById<Button>(R.id.btnViewMentors)
        val btnViewStudents = findViewById<Button>(R.id.btnViewStudents)
        val rvMentors = findViewById<RecyclerView>(R.id.rvMentorList)
        val rvStudents = findViewById<RecyclerView>(R.id.rvStudentList)

        val db = FirebaseFirestore.getInstance()

        // Fetch mentor count and display
        db.collection("mentors").get().addOnSuccessListener { documents ->
            tvMentorCount.text = "Total Mentors: ${documents.size()}"
        }

        // Fetch student count and display
        db.collection("students").get().addOnSuccessListener { documents ->
            tvStudentCount.text = "Total Students: ${documents.size()}"
        }

        btnViewMentors.setOnClickListener {
            // Hide the student list and show the mentor list
            rvStudents.visibility = View.GONE
            rvMentors.visibility = View.VISIBLE

            // Fetch mentor data from Firestore and set adapter
            db.collection("mentors").get().addOnSuccessListener { documents ->
                val mentorList = documents.map { it.getString("name") ?: "Unnamed Mentor" }
                rvMentors.layoutManager = LinearLayoutManager(this)
                rvMentors.adapter = UserAdapter(mentorList) // Only pass the mentorList now
            }
        }

        btnViewStudents.setOnClickListener {
            // Hide the mentor list and show the student list
            rvMentors.visibility = View.GONE
            rvStudents.visibility = View.VISIBLE

            // Fetch student data from Firestore and set adapter
            db.collection("students").get().addOnSuccessListener { documents ->
                val studentList = documents.map { it.getString("name") ?: "Unnamed Student" }
                rvStudents.layoutManager = LinearLayoutManager(this)
                rvStudents.adapter = UserAdapter(studentList) // Only pass the studentList now
            }
        }

        // Edge-to-edge padding handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
