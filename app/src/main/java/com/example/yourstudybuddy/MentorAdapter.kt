package com.example.yourstudybuddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.R
import com.example.yourstudybuddy.models.Mentor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MentorAdapter(
    private val mentorList: MutableList<Mentor>,
    private val onBookClick: (Mentor) -> Unit
) : RecyclerView.Adapter<MentorAdapter.MentorViewHolder>() {

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
        holder.experienceTextView.text = mentor.experience ?: "5+ years of experience"
        holder.aboutTextView.text = mentor.about.ifEmpty { "Not Available" }

        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val todayAvailability = mentor.availability?.get(currentDay) as? Map<*, *>
        holder.availabilityTextView.text = if (todayAvailability is Map<*, *>) {
            "${todayAvailability["startTime"]} - ${todayAvailability["endTime"]}"
        } else {
            "Not Available Today"
        }

        val price = mentor.price ?: "1000"
        holder.feeTextView.text = "₹$price/session"

        holder.profileImageView.setImageResource(R.drawable.ic_profile)

        holder.bookButton.setOnClickListener {
            onBookClick(mentor)
        }
    }

    override fun getItemCount(): Int = mentorList.size

    fun addMentors(newMentors: List<Mentor>) {
        mentorList.addAll(newMentors)
        notifyDataSetChanged()
    }
}