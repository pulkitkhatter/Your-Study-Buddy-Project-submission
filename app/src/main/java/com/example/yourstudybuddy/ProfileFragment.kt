package com.example.yourstudybuddy

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var phoneTextView: TextView
    private lateinit var departmentTextView: TextView
    private lateinit var cityStateTextView: TextView
    private lateinit var sessionsTextView: TextView
    private lateinit var walletTextView: TextView
    private lateinit var logoutButton: Button

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        nameTextView = view.findViewById(R.id.nameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        phoneTextView = view.findViewById(R.id.phoneTextView)
        departmentTextView = view.findViewById(R.id.departmentTextView)
        cityStateTextView = view.findViewById(R.id.cityStateTextView)
        sessionsTextView = view.findViewById(R.id.sessionsTextView)
        walletTextView = view.findViewById(R.id.walletTextView)
        logoutButton = view.findViewById(R.id.logoutButton)

        loadUserProfile()

        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), SignInScreen::class.java))
            requireActivity().finish()
        }

        setupEditListeners()

        return view
    }

    private fun loadUserProfile() {
        val email = auth.currentUser?.email
        if (email != null) {
            db.collection("students").document(email).addSnapshotListener { document, error ->
                if (error != null || document == null) return@addSnapshotListener

                if (document.exists()) {
                    nameTextView.text = document.getString("name") ?: "Not Provided"
                    emailTextView.text = email
                    phoneTextView.text = document.getString("phone") ?: "Not Provided"
                    departmentTextView.text = document.getString("department") ?: "Not Provided"
                    val city = document.getString("city") ?: "Not Provided"
                    val state = document.getString("state") ?: "Not Provided"
                    cityStateTextView.text = "$city, $state"
                    sessionsTextView.text = "Total Sessions Booked: ${document.getLong("sessionsBooked") ?: 0}"

                    val walletBalance = document.getLong("walletBalance") ?: 0
                    walletTextView.text = "Wallet: ₹$walletBalance"
                }
            }
        }
    }

    private fun setupEditListeners() {
        nameTextView.setOnClickListener { showEditDialog("name", "Enter your name", nameTextView) }
        phoneTextView.setOnClickListener { showEditDialog("phone", "Enter your phone", phoneTextView) }
        departmentTextView.setOnClickListener { showEditDialog("department", "Enter your department", departmentTextView) }
        cityStateTextView.setOnClickListener { showEditDialog("cityState", "Enter City, State", cityStateTextView) }

        walletTextView.setOnClickListener {
            val intent = Intent(requireContext(), WalletActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showEditDialog(field: String, hint: String, textView: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update $field")

        val input = EditText(requireContext())
        input.hint = hint
        builder.setView(input)

        builder.setPositiveButton("Save") { _, _ ->
            val newValue = input.text.toString().trim()
            if (newValue.isNotEmpty()) {
                updateUserProfile(field, newValue, textView)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun updateUserProfile(field: String, value: String, textView: TextView) {
        val email = auth.currentUser?.email
        if (email != null) {
            val fieldToUpdate = when (field) {
                "cityState" -> {
                    val parts = value.split(",").map { it.trim() }
                    mapOf("city" to (parts.getOrNull(0) ?: ""), "state" to (parts.getOrNull(1) ?: ""))
                }
                else -> mapOf(field to value)
            }

            db.collection("students").document(email)
                .update(fieldToUpdate)
                .addOnSuccessListener {
                    textView.text = value
                    Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
