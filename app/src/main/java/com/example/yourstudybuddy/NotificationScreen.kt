package com.example.yourstudybuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationScreen : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_screen)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Sample Notifications (Simple Messages)
        val notifications = listOf(
            "Welcome to StudyBuddy! Start your journey today.",
            "New study materials available in the library section.",
            "Reminder: Your scheduled session starts in 1 hour.",
            "Check out the latest tips from top mentors.",
            "Update: New courses have been added!",
            "Study streak unlocked! Keep going strong 💪",
            "Your daily quiz is now available. Give it a try!",
            "Your mentor has replied to your question.",
            "Upcoming Event: Live Q&A with industry experts!",
            "New feature alert! Dark mode is now available."
        )

        adapter = NotificationAdapter(notifications)
        recyclerView.adapter = adapter
    }
}
