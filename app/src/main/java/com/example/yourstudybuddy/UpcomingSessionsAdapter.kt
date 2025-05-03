package com.example.yourstudybuddy.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstudybuddy.R
import com.example.yourstudybuddy.models.Booking

class UpcomingSessionsAdapter(
    private val context: Context,
    private val bookings: List<Booking>
) : RecyclerView.Adapter<UpcomingSessionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mentorName: TextView = view.findViewById(R.id.mentorName)
        val sessionDate: TextView = view.findViewById(R.id.sessionDate)
        val sessionTime: TextView = view.findViewById(R.id.sessionTime)
        val sessionPrice: TextView = view.findViewById(R.id.sessionPrice)
        val joinMeetingButton: Button = view.findViewById(R.id.joinMeetingButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val booking = bookings[position]

        holder.mentorName.text = booking.mentorName
        holder.sessionDate.text = "Date: ${booking.sessionDate}"
        holder.sessionTime.text = "Time: ${booking.sessionTime}"
        holder.sessionPrice.text = "Price: ₹${booking.price}"

        // Open Google Meet on Click
        holder.joinMeetingButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://meet.google.com/qku-voof-tmz"))
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = bookings.size
}
