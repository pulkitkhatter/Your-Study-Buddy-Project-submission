package com.example.yourstudybuddy

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MentorScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var logoutButton: Button
    private lateinit var savePriceButton: Button
    private lateinit var priceInput: EditText
    private lateinit var checkBoxes: List<CheckBox>
    private lateinit var dayTextViews: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mentor_screen)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        logoutButton = findViewById(R.id.logoutButton)
        savePriceButton = findViewById(R.id.saveAvailabilityButton)
        priceInput = findViewById(R.id.priceInput)

        checkBoxes = listOf(
            findViewById(R.id.mondayCheck),
            findViewById(R.id.tuesdayCheck),
            findViewById(R.id.wednesdayCheck),
            findViewById(R.id.thursdayCheck),
            findViewById(R.id.fridayCheck),
            findViewById(R.id.saturdayCheck),
            findViewById(R.id.sundayCheck)
        )

        dayTextViews = listOf(
            findViewById(R.id.mondayAvailability),
            findViewById(R.id.tuesdayAvailability),
            findViewById(R.id.wednesdayAvailability),
            findViewById(R.id.thursdayAvailability),
            findViewById(R.id.fridayAvailability),
            findViewById(R.id.saturdayAvailability),
            findViewById(R.id.sundayAvailability)
        )

        loadAvailability()
        loadPricing()

        checkBoxes.forEachIndexed { index, checkBox ->
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    showTimeDialog(index)
                } else {
                    saveAvailability(index, null, null)
                }
            }
        }

        savePriceButton.setOnClickListener { savePrice() }
        logoutButton.setOnClickListener { logout() }
    }

    private fun showTimeDialog(index: Int) {
        val timeSlots = listOf(
            "12:00 AM", "1:00 AM", "2:00 AM", "3:00 AM", "4:00 AM", "5:00 AM", "6:00 AM",
            "7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM",
            "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM",
            "9:00 PM", "10:00 PM", "11:00 PM"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Availability")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val startTimeSpinner = Spinner(this)
        val endTimeSpinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        startTimeSpinner.adapter = adapter
        endTimeSpinner.adapter = adapter

        layout.addView(TextView(this).apply { text = "From:" })
        layout.addView(startTimeSpinner)
        layout.addView(TextView(this).apply { text = "To:" })
        layout.addView(endTimeSpinner)
        builder.setView(layout)

        builder.setPositiveButton("Save") { _, _ ->
            val startTime = startTimeSpinner.selectedItem.toString()
            val endTime = endTimeSpinner.selectedItem.toString()
            if (isValidTimeRange(startTime, endTime)) {
                saveAvailability(index, startTime, endTime)
            } else {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                checkBoxes[index].isChecked = false
            }
        }

        builder.setNegativeButton("Cancel") { _, _ ->
            checkBoxes[index].isChecked = false
        }

        builder.show()
    }

    private fun isValidTimeRange(startTime: String, endTime: String): Boolean {
        val timeMap = listOf(
            "12:00 AM", "1:00 AM", "2:00 AM", "3:00 AM", "4:00 AM", "5:00 AM", "6:00 AM",
            "7:00 AM", "8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 PM", "1:00 PM",
            "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM", "6:00 PM", "7:00 PM", "8:00 PM",
            "9:00 PM", "10:00 PM", "11:00 PM"
        ).withIndex().associate { it.value to it.index }
        return timeMap[startTime]!! < timeMap[endTime]!!
    }

    private fun saveAvailability(index: Int, startTime: String?, endTime: String?) {
        val mentorEmail = auth.currentUser?.email ?: return
        val day = checkBoxes[index].text.toString()

        val availabilityData = if (startTime != null && endTime != null) {
            mapOf("startTime" to startTime, "endTime" to endTime)
        } else {
            "Unavailable"
        }

        val data = hashMapOf<String, Any>(day to availabilityData)

        val docRef = firestore.collection("MentorAvailability").document(mentorEmail)

        // Use set() with merge instead of update()
        docRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                dayTextViews[index].text = startTime?.let { "$it - $endTime" } ?: "Unavailable"
                Toast.makeText(this, "Availability saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save availability: ${e.message}", Toast.LENGTH_SHORT).show()
                // Revert checkbox state if save fails
                checkBoxes[index].isChecked = startTime != null
            }
    }

    private fun savePrice() {
        val price = priceInput.text.toString().trim()
        if (price.isEmpty()) {
            Toast.makeText(this, "Please enter a price", Toast.LENGTH_SHORT).show()
            return
        }

        val mentorEmail = auth.currentUser?.email ?: return
        val data = hashMapOf(
            "price" to price
        )

        firestore.collection("MentorPricing").document(mentorEmail)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Price saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save price: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAvailability() {
        val email = auth.currentUser?.email ?: return

        firestore.collection("MentorAvailability").document(email)
            .get()
            .addOnSuccessListener { document ->
                document.data?.forEach { (day, value) ->
                    val index = checkBoxes.indexOfFirst { it.text.toString() == day }
                    if (index != -1) {
                        checkBoxes[index].setOnCheckedChangeListener(null) // Disable listener temporarily

                        if (value is Map<*, *>) {
                            checkBoxes[index].isChecked = true
                            dayTextViews[index].text = "${value["startTime"]} - ${value["endTime"]}"
                        } else {
                            checkBoxes[index].isChecked = false
                            dayTextViews[index].text = "Unavailable"
                        }

                        checkBoxes[index].setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                showTimeDialog(index)
                            } else {
                                saveAvailability(index, null, null)
                            }
                        }
                    }
                }
            }
    }

    private fun loadPricing() {
        val email = auth.currentUser?.email ?: return
        firestore.collection("MentorPricing").document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    priceInput.setText(document.getString("price") ?: "")
                }
            }
    }

    private fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        startActivity(Intent(this, SignInScreen::class.java))
        finish()
    }
}