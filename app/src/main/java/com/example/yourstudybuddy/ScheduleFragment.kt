package com.example.yourstudybuddy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.adapters.MentorAdapter
import com.example.yourstudybuddy.models.Mentor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: MentorAdapter
    private val allMentors = mutableListOf<Mentor>()  // Stores all mentors
    private val displayedMentors = mutableListOf<Mentor>()  // Mentors currently displayed

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.searchView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MentorAdapter(displayedMentors)
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
                        // Set default experience
                        mentor.experience = "5+ years of experience"

                        // Load additional data
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

                db.collection("MentorPricing").document(documentId)
                    .get()
                    .addOnSuccessListener { pricingDoc ->
                        mentor.price = pricingDoc.getString("price") ?: "Not Assigned"

                        // Add to all mentors list
                        allMentors.add(mentor)

                        // Update displayed mentors (initially show all)
                        if (searchView.query.isEmpty()) {
                            displayedMentors.add(mentor)
                            adapter.notifyItemInserted(displayedMentors.size - 1)
                        }
                    }
            }
    }

    private fun filterMentors(query: String) {
        displayedMentors.clear()

        if (query.isEmpty()) {
            // Show all mentors when search is empty
            displayedMentors.addAll(allMentors)
        } else {
            // Filter mentors whose name contains the query (case insensitive)
            val filtered = allMentors.filter {
                it.name.contains(query, ignoreCase = true)
            }
            displayedMentors.addAll(filtered)
        }

        adapter.notifyDataSetChanged()
    }
}