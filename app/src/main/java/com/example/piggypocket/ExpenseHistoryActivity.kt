package com.example.piggypocket

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseHistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var barChart: BarChart

    private var startDate: Long? = null
    private var endDate: Long? = null
    private var selectedCategoryId: Int? = null
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_history)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        barChart = findViewById(R.id.barChart)

        setupBarChart()
        setupFilters()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rvCategorySummary = findViewById<RecyclerView>(R.id.rvCategorySummary)
        val rvAllExpenses = findViewById<RecyclerView>(R.id.rvAllExpenses)

        rvCategorySummary.layoutManager = LinearLayoutManager(this)
        rvAllExpenses.layoutManager = LinearLayoutManager(this)

        loadHistoryData(rvCategorySummary, rvAllExpenses)
    }

    private fun setupFilters() {
        val btnFilter = findViewById<ImageButton>(R.id.btnFilter)
        val clFilterSection = findViewById<View>(R.id.clFilterSection)
        val tvStartDate = findViewById<TextView>(R.id.tvStartDate)
        val tvEndDate = findViewById<TextView>(R.id.tvEndDate)
        val spCategory = findViewById<Spinner>(R.id.spCategory)
        val btnClearFilters = findViewById<Button>(R.id.btnClearFilters)

        btnFilter.setOnClickListener {
            clFilterSection.visibility = if (clFilterSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        val dateSetListener = { isStart: Boolean ->
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth, if (isStart) 0 else 23, if (isStart) 0 else 59, if (isStart) 0 else 59)
                val dateLong = selectedCal.timeInMillis
                val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                
                if (isStart) {
                    startDate = dateLong
                    tvStartDate.text = format.format(Date(dateLong))
                } else {
                    endDate = dateLong
                    tvEndDate.text = format.format(Date(dateLong))
                }
                loadHistoryData(findViewById(R.id.rvCategorySummary), findViewById(R.id.rvAllExpenses))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvStartDate.setOnClickListener { dateSetListener(true) }
        tvEndDate.setOnClickListener { dateSetListener(false) }

        lifecycleScope.launch {
            categories = db.categoryDao().getAllCategories(sessionManager.getUserId())
            val categoryNames = mutableListOf("All Categories")
            categoryNames.addAll(categories.map { it.name })
            
            val adapter = ArrayAdapter(this@ExpenseHistoryActivity, R.layout.item_category_spinner, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCategory.adapter = adapter
            
            spCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategoryId = if (position == 0) null else categories[position - 1].id
                    loadHistoryData(findViewById(R.id.rvCategorySummary), findViewById(R.id.rvAllExpenses))
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }
        }

        btnClearFilters.setOnClickListener {
            startDate = null
            endDate = null
            selectedCategoryId = null
            tvStartDate.text = "Select Date"
            tvEndDate.text = "Select Date"
            spCategory.setSelection(0)
            loadHistoryData(findViewById(R.id.rvCategorySummary), findViewById(R.id.rvAllExpenses))
        }
    }

    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.legend.isEnabled = false

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.GRAY
        xAxis.textSize = 11f
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Spent", "Budget", "Income"))

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LTGRAY
        leftAxis.textColor = Color.GRAY
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f

        barChart.axisRight.isEnabled = false
    }

    private fun loadHistoryData(rvCategorySummary: RecyclerView, rvAllExpenses: RecyclerView) {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val budget = user?.monthlyBudget ?: 0.0
            val income = user?.income ?: 0.0

            var allExpenses = db.expenseDao().getAllExpenses(userId)
            
            // Apply filters
            if (startDate != null) {
                allExpenses = allExpenses.filter { it.date >= startDate!! }
            }
            if (endDate != null) {
                allExpenses = allExpenses.filter { it.date <= endDate!! }
            }
            if (selectedCategoryId != null) {
                allExpenses = allExpenses.filter { it.categoryId == selectedCategoryId }
            }
            
            allExpenses = allExpenses.sortedByDescending { it.date }
            val totalSpent = allExpenses.sumOf { it.amount }

            // Prepare bar chart data
            val entries = mutableListOf<BarEntry>()
            entries.add(BarEntry(0f, totalSpent.toFloat()))
            entries.add(BarEntry(1f, budget.toFloat()))
            entries.add(BarEntry(2f, income.toFloat()))

            val allCategories = db.categoryDao().getAllCategories(userId)
            val categoryMap = allCategories.associate { it.id to it.name }

            // Category summary
            val categoryWithSpent = allCategories.map { category ->
                val spent = allExpenses.filter { it.categoryId == category.id }.sumOf { it.amount }
                CategoryWithSpent(category, spent)
            }.filter { it.spent > 0 }

            runOnUiThread {
                findViewById<TextView>(R.id.tvTotalAmount).text = "R${String.format("%.2f", totalSpent)}"
                findViewById<TextView>(R.id.tvCount).text = "${allExpenses.size} expenses"

                rvCategorySummary.adapter = CategorySummaryAdapter(categoryWithSpent)
                rvAllExpenses.adapter = AllExpensesAdapter(allExpenses, categoryMap) { expense ->
                    showDeleteConfirmation(expense)
                }

                val dataSet = BarDataSet(entries, "Financial Overview")
                dataSet.colors = listOf(
                    Color.parseColor("#E91E63"), // Spent - Pink
                    Color.parseColor("#FF9800"), // Budget - Orange
                    Color.parseColor("#4CAF50")  // Income - Green
                )
                dataSet.setDrawValues(true)
                dataSet.valueTextColor = Color.DKGRAY
                dataSet.valueTextSize = 10f

                val barData = BarData(dataSet)
                barData.barWidth = 0.6f
                
                barChart.data = barData
                barChart.animateY(1000)
                barChart.invalidate()
            }
        }
    }

    private fun showDeleteConfirmation(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.expenseDao().delete(expense)
                    loadHistoryData(findViewById(R.id.rvCategorySummary), findViewById(R.id.rvAllExpenses))
                    Toast.makeText(this@ExpenseHistoryActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
