package com.example.piggypocket

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch

class SpendingSummaryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var pieChart: PieChart
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvSpendingInsight: TextView
    private lateinit var rvBreakdown: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_summary)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        pieChart = findViewById(R.id.pieChart)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvSpendingInsight = findViewById(R.id.tvSpendingInsight)
        rvBreakdown = findViewById(R.id.rvBreakdown)

        rvBreakdown.layoutManager = LinearLayoutManager(this)

        setupPieChart()
        loadSpendingData()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setupPieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.holeRadius = 58f
        pieChart.setDrawCenterText(false)
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.isHighlightPerTapEnabled = true
        pieChart.setDrawEntryLabels(false)
        
        pieChart.legend.isEnabled = true
        pieChart.legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        pieChart.legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        pieChart.legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        pieChart.legend.setDrawInside(false)
        pieChart.legend.xEntrySpace = 20f
        pieChart.legend.yEntrySpace = 5f
        pieChart.legend.isWordWrapEnabled = true
    }

    private fun loadSpendingData() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val expenses = db.expenseDao().getAllExpenses(userId)
            val categories = db.categoryDao().getAllCategories(userId).associateBy { it.id }

            val categoryTotals = mutableMapOf<String, Double>()
            var totalSpending = 0.0

            for (expense in expenses) {
                val categoryName = categories[expense.categoryId]?.name ?: "Unknown"
                categoryTotals[categoryName] = categoryTotals.getOrDefault(categoryName, 0.0) + expense.amount
                totalSpending += expense.amount
            }

            tvTotalAmount.text = "R %.2f".format(totalSpending)

            val entries = ArrayList<PieEntry>()
            categoryTotals.forEach { (name, amount) ->
                entries.add(PieEntry(amount.toFloat(), name))
            }

            val dataSet = PieDataSet(entries, "Spending Categories")
            dataSet.sliceSpace = 3f
            dataSet.selectionShift = 5f

            val colors = ArrayList<Int>()
            val chartColors = listOf(
                ColorTemplate.VORDIPLOM_COLORS,
                ColorTemplate.JOYFUL_COLORS,
                ColorTemplate.COLORFUL_COLORS,
                ColorTemplate.LIBERTY_COLORS,
                ColorTemplate.PASTEL_COLORS
            )
            for (colorArray in chartColors) {
                for (c in colorArray) colors.add(c)
            }
            dataSet.colors = colors

            val data = PieData(dataSet)
            data.setDrawValues(false)
            pieChart.data = data

            pieChart.highlightValues(null)
            pieChart.invalidate()

            // Update Detailed Breakdown and Insights
            val breakdownItems = mutableListOf<CategoryBreakdownAdapter.CategoryBreakdownItem>()
            var maxSpending = 0.0
            var topCategory = ""

            categoryTotals.toList().sortedByDescending { it.second }.forEachIndexed { index, (name, amount) ->
                val percentage = if (totalSpending > 0) (amount / totalSpending) * 100 else 0.0
                val color = colors[index % colors.size]
                breakdownItems.add(CategoryBreakdownAdapter.CategoryBreakdownItem(name, amount, percentage, color))

                if (amount > maxSpending) {
                    maxSpending = amount
                    topCategory = name
                }
            }

            rvBreakdown.adapter = CategoryBreakdownAdapter(breakdownItems)

            if (topCategory.isNotEmpty()) {
                val amountText = "R %.2f".format(maxSpending)
                val fullText = "Your highest spending category is $topCategory, accounting for $amountText this month."
                val spannable = SpannableString(fullText)
                
                val catStart = fullText.indexOf(topCategory)
                if (catStart != -1) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), catStart, catStart + topCategory.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF8F00")), catStart, catStart + topCategory.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                
                val amtStart = fullText.indexOf(amountText)
                if (amtStart != -1) {
                    spannable.setSpan(StyleSpan(Typeface.BOLD), amtStart, amtStart + amountText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                
                tvSpendingInsight.text = spannable
            } else {
                tvSpendingInsight.text = "No spending data available for this month."
            }
        }
    }
}