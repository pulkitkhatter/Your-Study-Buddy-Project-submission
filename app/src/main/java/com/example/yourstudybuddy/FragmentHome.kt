package com.example.yourstudybuddy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.adapters.UpcomingSessionsAdapter
import com.example.yourstudybuddy.models.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FragmentHome : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noDataText: TextView
    private lateinit var upcomingSessionsAdapter: UpcomingSessionsAdapter
    private val upcomingSessionsList = mutableListOf<Booking>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home2, container, false)
        setupViews(view)
        loadUpcomingSessions()
        return view
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        noDataText = view.findViewById(R.id.noDataText)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        upcomingSessionsAdapter = UpcomingSessionsAdapter(requireContext(), upcomingSessionsList)
        recyclerView.adapter = upcomingSessionsAdapter
    }

    private fun loadUpcomingSessions() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showMessage("Please sign in to view sessions")
            return
        }

        showLoading()

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("bookings")
            .whereEqualTo("studentEmail", currentUser.email)
            .whereEqualTo("status", "confirmed")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val filteredList = querySnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.takeIf { it.sessionDate >= currentDate }
                }.sortedBy { it.sessionDate }

                upcomingSessionsList.clear()
                upcomingSessionsList.addAll(filteredList)
                showSessions()
            }
            .addOnFailureListener { exception ->
                showMessage("Failed to load sessions")
                Log.e("FragmentHome", "Error loading sessions", exception)
            }
    }

    private fun showLoading() {
        noDataText.text = "Loading sessions..."
        noDataText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showMessage(message: String) {
        noDataText.text = message
        noDataText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showSessions() {
        noDataText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        upcomingSessionsAdapter.notifyDataSetChanged()
    }
}
