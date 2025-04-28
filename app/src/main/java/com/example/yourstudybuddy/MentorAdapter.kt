package com.example.yourstudybuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.R
import com.example.yourstudybuddy.models.Mentor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MentorAdapter(private val mentorList: MutableList<Mentor>) :
    RecyclerView.Adapter<MentorAdapter.MentorViewHolder>() {

    private val random = Random()
    private val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    class MentorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val expertiseTextView: TextView = view.findViewById(R.id.expertiseTextView)
        val experienceTextView: TextView = view.findViewById(R.id.experienceTextView)
        val aboutTextView: TextView = view.findViewById(R.id.aboutTextView)
        val availabilityTextView: TextView = view.findViewById(R.id.availabilityTextView)
        val feeTextView: TextView = view.findViewById(R.id.feeTextView)
        val bookButton: Button = view.findViewById(R.id.bookButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MentorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mentor_item, parent, false)
        return MentorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MentorViewHolder, position: Int) {
        val mentor = mentorList[position]

        holder.nameTextView.text = mentor.name.ifEmpty { "Not Available" }
        holder.expertiseTextView.text = mentor.department.ifEmpty { "Not Available" }

        // Generate random experience between 2-10 years
        val experience = (random.nextInt(9) + 2).toString() + " years"
        holder.experienceTextView.text = experience

        holder.aboutTextView.text = mentor.about.ifEmpty { "Not Available" }

        // Display today's availability if available
        val todayAvailability = mentor.availability?.get(currentDay)
        holder.availabilityTextView.text = if (todayAvailability is Map<*, *>) {
            "${todayAvailability["startTime"]} - ${todayAvailability["endTime"]}"
        } else {
            "Not Available Today"
        }

        // Display pricing (use 1000 if not set)
        val price = mentor.price ?: "1000"
        holder.feeTextView.text = "₹$price/session"

        holder.profileImageView.setImageResource(R.drawable.ic_profile)

        holder.bookButton.setOnClickListener {
            bookSession(holder, mentor, price)
        }
    }

    private fun bookSession(holder: MentorViewHolder, mentor: Mentor, price: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(holder.itemView.context, "Please sign in to book a session", Toast.LENGTH_SHORT).show()
            return
        }

        val priceValue = price.toDoubleOrNull() ?: 1000.0

        // Check wallet balance first
        db.collection("students").document(currentUser.email ?: "")
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(holder.itemView.context, "User data not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val walletBalance = document.getDouble("walletBalance") ?: 0.0

                if (walletBalance < priceValue) {
                    Toast.makeText(holder.itemView.context, "Insufficient funds in wallet", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Deduct from wallet
                db.collection("students").document(currentUser.email ?: "")
                    .update("walletBalance", walletBalance - priceValue)
                    .addOnSuccessListener {
                        // Create booking record
                        val bookingData = hashMapOf(
                            "mentorId" to mentor.id,
                            "mentorName" to mentor.name,
                            "studentEmail" to currentUser.email,
                            "price" to priceValue,
                            "bookingDate" to System.currentTimeMillis(),
                            "status" to "confirmed",
                            "sessionDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )

                        db.collection("bookings").add(bookingData)
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Session booked successfully!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(holder.itemView.context, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Refund if booking creation failed
                                db.collection("students").document(currentUser.email ?: "")
                                    .update("walletBalance", walletBalance)
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(holder.itemView.context, "Payment failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(holder.itemView.context, "Failed to check wallet balance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = mentorList.size

    fun addMentors(newMentors: List<Mentor>) {
        mentorList.addAll(newMentors)
        notifyDataSetChanged()
    }
}