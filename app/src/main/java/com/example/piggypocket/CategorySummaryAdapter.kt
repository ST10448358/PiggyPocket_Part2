package com.example.piggypocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategorySummaryAdapter(private val categories: List<CategoryWithSpent>) :
    RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvCatIcon)
        val tvName: TextView = view.findViewById(R.id.tvCatName)
        val tvAmount: TextView = view.findViewById(R.id.tvCatAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        
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
    }

    override fun getItemCount() = categories.size

    private fun isEmoji(s: String): Boolean {
        return s.isNotEmpty() && (Character.getType(s.codePointAt(0)) == Character.SURROGATE.toInt() || 
                Character.getType(s.codePointAt(0)) == Character.OTHER_SYMBOL.toInt())
    }
}