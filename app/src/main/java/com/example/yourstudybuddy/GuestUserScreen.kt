package com.example.yourstudybuddy

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import me.relex.circleindicator.CircleIndicator

class GuestUserScreen : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var imageList: List<Int>
    private lateinit var indicator: CircleIndicator
    private lateinit var recyclerViewFAQ: RecyclerView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_user_screen)

        // Initialize viewPager
        viewPager = findViewById(R.id.idViewPager)
        indicator = findViewById(R.id.idIndicator)
        recyclerViewFAQ = findViewById(R.id.recyclerViewFAQ)

        // Initialize and add images
        imageList = listOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image2
        )

        // Initialize adapter and set it to ViewPager
        viewPagerAdapter = ViewPagerAdapter(this, imageList)
        viewPager.adapter = viewPagerAdapter
        indicator.setViewPager(viewPager)

        // Auto-slide images every 3 seconds
        autoSlideImages()

        // Setup RecyclerView for FAQ
        // Define FAQ items properly as List<FAQItem>
        val faqList = listOf(
            FAQItem(
                "What is Kats?",
                "Khatter All Technology Solutions - A tech education initiative"
            ),
            FAQItem(
                "Why choose Kats?",
                "Expert-led mentoring with proven learning methodologies"
            ),
            FAQItem(
                "About the Developer",
                "Pulkit Khatter - Solution Engineer & App Developer"
            ),
            FAQItem(
                "What is Your Study Buddy?",
                "Our flagship app connecting students with expert mentors"
            ),
            FAQItem(
                "Is there a free version?",
                "Yes, basic features are completely free to use"
            ),
            FAQItem(
                "How to contact support?",
                "Call: 9996734575 | Email: pulkitkhatter13@gmail.com"
            ),
            FAQItem(
                "Available subjects?",
                "All academic subjects + career guidance"
            ),
            FAQItem(
                "Mentor qualifications?",
                "Verified experts with teaching experience"
            ),
            FAQItem(
                "For Colleges/Universities",
                "Partner with us to enhance student success programs"
            ),
            FAQItem(
                "Device requirements?",
                "Android 9+ or iOS 13+ with internet connection"
            )
        )

// Fix RecyclerView Adapter
        recyclerViewFAQ.layoutManager = LinearLayoutManager(this)
        recyclerViewFAQ.adapter = FAQAdapter(faqList)


        recyclerViewFAQ.layoutManager = LinearLayoutManager(this)
        recyclerViewFAQ.adapter = FAQAdapter(faqList)
    }

    private fun autoSlideImages() {
        val runnable = object : Runnable {
            override fun run() {
                val nextPage = (viewPager.currentItem + 1) % imageList.size
                viewPager.setCurrentItem(nextPage, true)
                handler.postDelayed(this, 3000) // Change slide every 3 seconds
            }
        }
        handler.postDelayed(runnable, 3000)
    }
}
