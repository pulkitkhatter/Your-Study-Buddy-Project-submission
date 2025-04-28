package com.example.yourstudybuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.adapters.MentorAdapter
import com.example.yourstudybuddy.models.Mentor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.appcompat.widget.SearchView
import com.google.firebase.firestore.ktx.toObject

class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var adapter: MentorAdapter
    private val mentorList = mutableListOf<Mentor>()

    private val db = FirebaseFirestore.getInstance()
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private val batchSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_schedule, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.searchView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MentorAdapter(mentorList)
        recyclerView.adapter = adapter

        loadMentors()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (!isLoading && lastVisibleItem >= mentorList.size - 3) {
                    loadMentors()
                }
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchMentors(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    resetMentors()
                } else {
                    searchMentors(newText)
                }
                return false
            }
        })

        return view
    }

    private fun resetMentors() {
        mentorList.clear()
        lastVisible = null
        loadMentors()
    }

    private fun loadMentors() {
        if (isLoading) return
        isLoading = true

        var query: Query = db.collection("mentors").orderBy("name").limit(batchSize.toLong())
        lastVisible?.let {
            query = query.startAfter(it)
        }

        query.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                lastVisible = snapshot.documents[snapshot.size() - 1]

                snapshot.documents.forEach { document ->
                    val mentor = document.toObject<Mentor>()?.apply {
                        id = document.id
                        // Load availability data
                        db.collection("MentorAvailability").document(document.id)
                            .get()
                            .addOnSuccessListener { availabilityDoc ->
                                availability = availabilityDoc.data
                                // Load pricing data
                                db.collection("MentorPricing").document(document.id)
                                    .get()
                                    .addOnSuccessListener { pricingDoc ->
                                        // Set default price to 1000 if not set
                                        price = pricingDoc.getString("price") ?: "1000"
                                        // Add to list only after both availability and pricing are loaded
                                        if (!mentorList.contains(this)) {
                                            mentorList.add(this)
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                            }
                    }
                }
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    private fun searchMentors(searchText: String) {
        val query = db.collection("mentors")
            .orderBy("name")
            .startAt(searchText)
            .endAt(searchText + "\uf8ff")

        query.get().addOnSuccessListener { snapshot ->
            mentorList.clear()
            snapshot.documents.forEach { document ->
                val mentor = document.toObject<Mentor>()?.apply {
                    id = document.id
                    // Load availability data
                    db.collection("MentorAvailability").document(document.id)
                        .get()
                        .addOnSuccessListener { availabilityDoc ->
                            availability = availabilityDoc.data
                            // Load pricing data
                            db.collection("MentorPricing").document(document.id)
                                .get()
                                .addOnSuccessListener { pricingDoc ->
                                    // Set default price to 1000 if not set
                                    price = pricingDoc.getString("price") ?: "1000"
                                    mentorList.add(this)
                                    adapter.notifyDataSetChanged()
                                }
                        }
                }
            }
        }
    }
}