package com.example.yourstudybuddy.models

data class Booking(
    var id: String = "",
    val bookingDate: Long = 0,
    val mentorId: String = "",
    val mentorName: String = "",
    val price: Int = 0,
    val sessionDate: String = "",
    val status: String = "",
    val studentEmail: String = "",
    val sessionTime: String = "",  // Ensure this field exists
    val sessionPrice: String = ""
)