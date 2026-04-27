package com.example.piggypocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryProgressAdapter(private val categories: List<CategoryWithSpent>) :
    RecyclerView.Adapter<CategoryProgressAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvCatIcon)
        val tvName: TextView = view.findViewById(R.id.tvCatName)
        val tvAmount: TextView = view.findViewById(R.id.tvCatAmount)
        val pbProgress: ProgressBar = view.findViewById(R.id.pbCatProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_progress, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        
        // Extract emoji if present
        val nameParts = item.category.name.split(" ")
        if (nameParts.size > 1 && isEmoji(nameParts.last())) {
            holder.tvIcon.text = nameParts.last()
            holder.tvName.text = nameParts.dropLast(1).joinToString(" ")
        } else if (nameParts.isNotEmpty() && isEmoji(nameParts.first())) {
            holder.tvIcon.text = nameParts.first()
            holder.tvName.text = nameParts.drop(1).joinToString(" ")
        } else {
            holder.tvIcon.text = "💰"
            holder.tvName.text = item.category.name
        }

        holder.tvAmount.text = "R${String.format("%.2f", item.spent)}"
        
        if (item.spent > 0) {
            holder.pbProgress.visibility = View.VISIBLE
            
            val limit = if (item.category.budgetLimit > 0) item.category.budgetLimit else 1000.0
            val progress = ((item.spent / limit) * 100).toInt()
            
            holder.pbProgress.progress = progress.coerceIn(5, 100)
            
            if (item.category.budgetLimit > 0 && item.spent > item.category.budgetLimit) {
                holder.tvAmount.setTextColor(0xFFEF9A9A.toInt())
            } else {
                holder.tvAmount.setTextColor(0xFF757575.toInt())
            }
        } else {
            holder.pbProgress.visibility = View.INVISIBLE
            holder.pbProgress.progress = 0
            holder.tvAmount.setTextColor(0xFF757575.toInt())
        }
    }

    private fun isEmoji(s: String): Boolean {
        return s.isNotEmpty() && (Character.getType(s.codePointAt(0)).toByte() == Character.SURROGATE.toByte() || 
                Character.getType(s.codePointAt(0)).toByte() == Character.OTHER_SYMBOL.toByte())
    }

    override fun getItemCount() = categories.size
}

data class CategoryWithSpent(val category: Category, val spent: Double)