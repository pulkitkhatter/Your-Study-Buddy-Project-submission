package com.example.yourstudybuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FAQAdapter(private val faqList: List<FAQItem>) : RecyclerView.Adapter<FAQAdapter.FAQViewHolder>() {

    class FAQViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtQuestion: TextView = itemView.findViewById(R.id.txtQuestion)
        val txtAnswer: TextView = itemView.findViewById(R.id.txtAnswer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FAQViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_faq, parent, false)
        return FAQViewHolder(view)
    }

    override fun onBindViewHolder(holder: FAQViewHolder, position: Int) {
        val faq = faqList[position]

        holder.txtQuestion.text = faq.question
        holder.txtAnswer.text = faq.answer

        // Show answer based on isExpanded property
        holder.txtAnswer.visibility = if (faq.isExpanded) View.VISIBLE else View.GONE

        // Toggle visibility on click
        holder.itemView.setOnClickListener {
            faq.isExpanded = !faq.isExpanded
            notifyItemChanged(position) // Refresh only the clicked item
        }
    }

    override fun getItemCount(): Int = faqList.size
}
