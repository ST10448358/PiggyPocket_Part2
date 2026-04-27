package com.example.piggypocket

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(private val expenses: List<Expense>, private val categoryMap: Map<Int, String>) :
    RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvExpIcon)
        val tvName: TextView = view.findViewById(R.id.tvExpName)
        val tvCategory: TextView = view.findViewById(R.id.tvExpCategory)
        val tvAmount: TextView = view.findViewById(R.id.tvExpAmount)
        val tvDate: TextView = view.findViewById(R.id.tvExpDate)

        init {
            view.setOnClickListener {
                // Click listener set in onBindViewHolder
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = expenses[position]
        val categoryName = categoryMap[item.categoryId] ?: "Unknown"
        
        holder.tvName.text = item.description
        holder.tvCategory.text = categoryName
        holder.tvAmount.text = "-R${String.format("%.2f", item.amount)}"
        
        // Extract emoji from category name for the icon
        val emoji = categoryName.split(" ").firstOrNull { isEmoji(it) } ?: "💸"
        holder.tvIcon.text = emoji

        val format = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        holder.tvDate.text = format.format(Date(item.date))

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ExpenseDetailsActivity::class.java)
            intent.putExtra("EXPENSE_ID", item.id)
            context.startActivity(intent)
        }
    }

    private fun isEmoji(s: String): Boolean {
        return s.isNotEmpty() && (Character.getType(s.codePointAt(0)) == Character.SURROGATE.toInt() || 
                Character.getType(s.codePointAt(0)) == Character.OTHER_SYMBOL.toInt())
    }

    override fun getItemCount() = expenses.size
}