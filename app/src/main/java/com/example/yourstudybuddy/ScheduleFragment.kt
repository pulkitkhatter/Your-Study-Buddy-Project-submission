package com.example.yourstudybuddy

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.adapters.MentorAdapter
import com.example.yourstudybuddy.models.Mentor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: MentorAdapter
    private val allMentors = mutableListOf<Mentor>()
    private val displayedMentors = mutableListOf<Mentor>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.searchView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MentorAdapter(displayedMentors, this::showBookingDialog)
        recyclerView.adapter = adapter

        loadAllMentors()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMentors(newText.orEmpty())
                return true
            }
        })

        return view
    }

    private fun loadAllMentors() {
        db.collection("mentors").get()
            .addOnSuccessListener { snapshot ->
                allMentors.clear()
                displayedMentors.clear()

                snapshot.documents.forEach { document ->
                    val mentor = document.toObject<Mentor>()?.apply { id = document.id }

                    if (mentor != null) {
                        mentor.experience = "5+ years of experience"
                        loadMentorDetails(mentor, document.id)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ScheduleFragment", "Error loading mentors", e)
            }
    }

    private fun loadMentorDetails(mentor: Mentor, documentId: String) {
        db.collection("MentorAvailability").document(documentId)
            .get()
            .addOnSuccessListener { availabilityDoc ->
                mentor.availability = availabilityDoc.data

                val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                val todayAvailability = availabilityDoc.data?.get(currentDay) as? Map<*, *>
                val times = todayAvailability?.let {
                    listOf(
                        it["startTime"].toString(),
                        it["endTime"].toString()
                    )
                } ?: listOf("10:00 AM", "5:00 PM")

                mentor.availableTimes = generateTimeSlots(times[0], times[1])

                db.collection("MentorPricing").document(documentId)
                    .get()
                    .addOnSuccessListener { pricingDoc ->
                        mentor.price = pricingDoc.getString("price") ?: "Not Assigned"
                        allMentors.add(mentor)
                        if (searchView.query.isEmpty()) {
                            displayedMentors.add(mentor)
                            adapter.notifyItemInserted(displayedMentors.size - 1)
                        }
                    }
            }
    }

    private fun generateTimeSlots(start: String, end: String): List<String> {
        val timeSlots = mutableListOf<String>()
        try {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            val startTime = format.parse(start)
            val endTime = format.parse(end)

            var current = Calendar.getInstance().apply { time = startTime }
            val endCal = Calendar.getInstance().apply { time = endTime }

            while (current.before(endCal)) {
                timeSlots.add(format.format(current.time))
                current.add(Calendar.MINUTE, 30)
            }
        } catch (e: Exception) {
            Log.e("TimeSlot", "Error generating slots", e)
            return listOf("10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM",
                "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM",
                "2:00 PM", "2:30 PM", "3:00 PM", "3:30 PM",
                "4:00 PM", "4:30 PM", "5:00 PM")
        }
        return timeSlots
    }

    private fun filterMentors(query: String) {
        displayedMentors.clear()

        if (query.isEmpty()) {
            displayedMentors.addAll(allMentors)
        } else {
            val filtered = allMentors.filter {
                it.name.contains(query, ignoreCase = true)
            }
            displayedMentors.addAll(filtered)
        }

        adapter.notifyDataSetChanged()
    }

    fun showBookingDialog(mentor: Mentor) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Book Session with ${mentor.name}")

        val timeArray = mentor.availableTimes.toTypedArray()
        val timeSpinner = Spinner(requireContext())
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeArray).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeSpinner.adapter = adapter
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
            addView(TextView(requireContext()).apply {
                text = "Select Time:"
                setPadding(0, 0, 0, 20)
            })
            addView(timeSpinner)
        }

        builder.setView(layout)
        builder.setPositiveButton("Book") { dialog, _ ->
            val selectedTime = timeSpinner.selectedItem.toString()
            bookSession(mentor, selectedTime)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun bookSession(mentor: Mentor, time: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to book a session", Toast.LENGTH_SHORT).show()
            return
        }

        val priceValue = mentor.price?.toDoubleOrNull() ?: 1000.0

        db.collection("students").document(currentUser.email ?: "")
            .get()
            .addOnSuccessListener { document ->
                val walletBalance = document.getDouble("walletBalance") ?: 0.0

                if (walletBalance < priceValue) {
                    Toast.makeText(requireContext(), "Insufficient funds in wallet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("students").document(currentUser.email ?: "")
                    .update("walletBalance", walletBalance - priceValue)
                    .addOnSuccessListener {
                        val bookingData = hashMapOf(
                            "mentorId" to mentor.id,
                            "mentorName" to mentor.name,
                            "studentEmail" to currentUser.email,
                            "price" to priceValue,
                            "bookingDate" to System.currentTimeMillis(),
                            "status" to "confirmed",
                            "sessionDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                            "sessionTime" to time
                        )

                        db.collection("bookings").add(bookingData)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Session booked at $time!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                db.collection("students").document(currentUser.email ?: "")
                                    .update("walletBalance", walletBalance)
                            }
                    }
            }
    }
}