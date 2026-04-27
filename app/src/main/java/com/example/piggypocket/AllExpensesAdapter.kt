package com.example.piggypocket

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AllExpensesAdapter(
    private val expenses: List<Expense>, 
    private val categoryMap: Map<Int, String>,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<AllExpensesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvExpIcon)
        val tvName: TextView = view.findViewById(R.id.tvExpName)
        val tvDetails: TextView = view.findViewById(R.id.tvExpDetails)
        val tvAmount: TextView = view.findViewById(R.id.tvExpAmount)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = expenses[position]
        val categoryName = categoryMap[item.categoryId] ?: "Unknown"
        
        holder.tvName.text = item.description
        
        val format = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        val dateStr = format.format(Date(item.date))
        holder.tvDetails.text = "$categoryName - $dateStr"
        
        holder.tvAmount.text = "R${String.format("%.2f", item.amount)}"
        
        val emoji = categoryName.split(" ").firstOrNull { isEmoji(it) } ?: "💸"
        holder.tvIcon.text = emoji

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ExpenseDetailsActivity::class.java)
            intent.putExtra("EXPENSE_ID", item.id)
            context.startActivity(intent)
        }

        holder.ivDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount() = expenses.size

    private fun isEmoji(s: String): Boolean {
        return s.isNotEmpty() && (Character.getType(s.codePointAt(0)) == Character.SURROGATE.toInt() || 
                Character.getType(s.codePointAt(0)) == Character.OTHER_SYMBOL.toInt())
    }
}