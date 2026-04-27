package com.example.piggypocket

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryBreakdownAdapter(private val items: List<CategoryBreakdownItem>) :
    RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

    data class CategoryBreakdownItem(
        val name: String,
        val amount: Double,
        val percentage: Double,
        val color: Int
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vColorBar: View = view.findViewById(R.id.vColorBar)
        val tvCatName: TextView = view.findViewById(R.id.tvCatName)
        val tvCatPercentage: TextView = view.findViewById(R.id.tvCatPercentage)
        val tvCatAmount: TextView = view.findViewById(R.id.tvCatAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_breakdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.vColorBar.setBackgroundColor(item.color)
        holder.tvCatName.text = item.name
        holder.tvCatPercentage.text = "%.1f%% of total".format(item.percentage)
        holder.tvCatAmount.text = "R %.2f".format(item.amount)
    }

    override fun getItemCount() = items.size
}