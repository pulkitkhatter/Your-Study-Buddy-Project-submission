package com.example.adminapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnAddStudent = findViewById<Button>(R.id.btnAddStudent)
        val btnAddMentor = findViewById<Button>(R.id.btnAddMentor)
        val btnContinueAdmin = findViewById<Button>(R.id.btnContinueAdmin)

        btnAddStudent.setOnClickListener {
            startActivity(Intent(this, StudentFormActivity::class.java))
        }

        btnAddMentor.setOnClickListener {
            startActivity(Intent(this, MentorFormActivity::class.java))
        }

        btnContinueAdmin.setOnClickListener {
            startActivity(Intent(this, AdminScreenActivity::class.java))
        }
    }
}
