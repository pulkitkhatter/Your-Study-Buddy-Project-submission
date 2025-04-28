package com.example.yourstudybuddy

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import me.relex.circleindicator.CircleIndicator

class WalletActivity : AppCompatActivity() {

    private lateinit var walletBalanceTextView: TextView
    private lateinit var amountEditText: EditText
    private lateinit var viewPager: ViewPager
    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0
    private lateinit var imageList: List<Int>

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var walletBalance: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        walletBalanceTextView = findViewById(R.id.walletBalanceTextView)
        amountEditText = findViewById(R.id.amountEditText)
        val topUpButton = findViewById<Button>(R.id.topUpButton)

        fetchWalletBalance()

        // ViewPager for Image Slider
        viewPager = findViewById(R.id.idViewPager)
        val indicator = findViewById<CircleIndicator>(R.id.idIndicator)

        imageList = listOf(R.drawable.image1, R.drawable.image2, R.drawable.image2)
        val adapter = ViewPagerAdapter(this, imageList)
        viewPager.adapter = adapter
        indicator.setViewPager(viewPager)

        startAutoSlide()

        // Top-Up Buttons
        findViewById<Button>(R.id.topUp1000).setOnClickListener { setAmount(1000) }
        findViewById<Button>(R.id.topUp1500).setOnClickListener { setAmount(1500) }
        findViewById<Button>(R.id.topUp2000).setOnClickListener { setAmount(2000) }

        // Handle top-up process
        topUpButton.setOnClickListener {
            showPasswordConfirmationDialog()
        }
    }

    private fun fetchWalletBalance() {
        val email = auth.currentUser?.email
        if (email != null) {
            db.collection("students").document(email)
                .addSnapshotListener { document, error ->
                    if (error != null || document == null) return@addSnapshotListener

                    val balance = document.getLong("walletBalance")?.toInt() ?: 0
                    walletBalance = balance
                    updateWalletBalance()
                }
        }
    }

    private fun updateWalletBalance() {
        walletBalanceTextView.text = "\u20B9$walletBalance"
        walletBalanceTextView.setTextColor(if (walletBalance < 100) Color.RED else Color.parseColor("#6A0DAD"))
    }

    private fun setAmount(amount: Int) {
        amountEditText.setText(amount.toString())
    }

    private fun startAutoSlide() {
        val delay = 3000L
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (currentPage >= imageList.size) currentPage = 0
                viewPager.setCurrentItem(currentPage++, true)
                handler.postDelayed(this, delay)
            }
        }, delay)
    }

    private fun showPasswordConfirmationDialog() {
        val amount = amountEditText.text.toString().toIntOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordEditText = EditText(this)
        passwordEditText.hint = "Enter Password"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Payment")
        builder.setMessage("Enter your password to add ₹$amount to your wallet:")
        builder.setView(passwordEditText)
        builder.setPositiveButton("Confirm") { _, _ ->
            val password = passwordEditText.text.toString()
            if (password.isNotEmpty()) {
                verifyPassword(password, amount)
            } else {
                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun verifyPassword(password: String, amount: Int) {
        val user = auth.currentUser
        val email = user?.email

        if (user != null && email != null) {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    processPayment(amount)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Incorrect password! Try again.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun processPayment(amount: Int) {
        Toast.makeText(this, "Processing payment...", Toast.LENGTH_SHORT).show()

        // Simulate delay for payment processing
        Handler(Looper.getMainLooper()).postDelayed({
            val newBalance = walletBalance + amount
            updateWalletBalanceInFirebase(newBalance)
        }, 2000)
    }

    private fun updateWalletBalanceInFirebase(newBalance: Int) {
        val email = auth.currentUser?.email
        if (email != null) {
            db.collection("students").document(email)
                .update("walletBalance", newBalance)
                .addOnSuccessListener {
                    walletBalance = newBalance
                    updateWalletBalance()
                    Toast.makeText(this, "₹$walletBalance added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update wallet balance.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
