package com.example.yourstudybuddy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        val ivNotification = findViewById<ImageView>(R.id.ivNotification)

        // Set HomeFragment as default
        loadFragment(FragmentHome())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(FragmentHome())
                R.id.nav_chat -> loadFragment(ChatFragment())
                R.id.nav_schedule -> loadFragment(ScheduleFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // Navigate to Notification Screen when notification icon is clicked
        ivNotification.setOnClickListener {
            val intent = Intent(this, NotificationScreen::class.java)
            startActivity(intent)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
