package com.example.piggypocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryManageAdapter(
    private val categories: List<Category>,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryManageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvCatIcon)
        val tvName: TextView = view.findViewById(R.id.tvCatName)
        val tvBudget: TextView = view.findViewById(R.id.tvCatBudget)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_manage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        val nameParts = category.name.split(" ")
        if (nameParts.size > 1 && isEmoji(nameParts.last())) {
            holder.tvIcon.text = nameParts.last()
            holder.tvName.text = nameParts.dropLast(1).joinToString(" ")
        } else if (nameParts.isNotEmpty() && isEmoji(nameParts.first())) {
            holder.tvIcon.text = nameParts.first()
            holder.tvName.text = nameParts.drop(1).joinToString(" ")
        } else {
            holder.tvIcon.text = "💰"
            holder.tvName.text = category.name
        }

        holder.tvBudget.text = "Budget: R${String.format("%.2f", category.budgetLimit)}"
        
        holder.btnEdit.setOnClickListener { onEdit(category) }
        holder.btnDelete.setOnClickListener { onDelete(category) }
    }

    private fun isEmoji(s: String): Boolean {
        return s.isNotEmpty() && (Character.getType(s.codePointAt(0)).toByte() == Character.SURROGATE.toByte() || 
                Character.getType(s.codePointAt(0)).toByte() == Character.OTHER_SYMBOL.toByte())
    }

    override fun getItemCount() = categories.size
}