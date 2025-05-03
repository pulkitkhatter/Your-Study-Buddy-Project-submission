package com.example.yourstudybuddy.models

data class Mentor(
    var id: String = "",
    var name: String = "",
    var department: String = "",
    var about: String = "",
    var availability: Map<String, Any>? = null,
    var price: String? = null,
    var experience: String? = null,
    var availableTimes: List<String> = listOf()
) {
    fun getFormattedName(): String {
        return name.replaceFirstChar { it.uppercase() }
    }
}