package com.example.piggypocket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Main dashboard screen that provides an overview of the user's financial status,
 * including remaining budget, total expenses, income, and recent transactions.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    private lateinit var tvUsername: TextView
    private lateinit var tvRemainingAmount: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvAchievementCount: TextView
    private lateinit var rvCategoryProgress: RecyclerView
    private lateinit var rvRecentExpenses: RecyclerView

    private lateinit var tvNoExpenses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize database and session management
        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Initialize UI components
        tvUsername = findViewById(R.id.tvUsername)
        tvRemainingAmount = findViewById(R.id.tvRemainingAmount)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvAchievementCount = findViewById(R.id.tvAchievementCount)
        rvCategoryProgress = findViewById(R.id.rvCategoryProgress)
        rvRecentExpenses = findViewById(R.id.rvRecentExpenses)
        tvNoExpenses = findViewById(R.id.tvNoExpenses)

        // Setup RecyclerViews with Linear Layout Managers
        rvCategoryProgress.layoutManager = LinearLayoutManager(this)
        rvRecentExpenses.layoutManager = LinearLayoutManager(this)

        // Navigation: Edit Budget (Settings)
        findViewById<View>(R.id.ivEditBudget).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Navigation: Add New Expense
        findViewById<LinearLayout>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Navigation: View Spending Summary (Analysis)
        findViewById<View>(R.id.tvAnalyze)?.setOnClickListener {
            startActivity(Intent(this, SpendingSummaryActivity::class.java))
        }

        // Navigation: View All Expenses
        findViewById<View>(R.id.tvViewAll)?.setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        // Navigation: History Screen
        findViewById<LinearLayout>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, ExpenseHistoryActivity::class.java))
        }

        // Navigation: Categories Screen
        findViewById<LinearLayout>(R.id.btnCategories).setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        // Navigation: Goals Screen
        findViewById<LinearLayout>(R.id.btnGoals).setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }

        // Navigation: Settings Screen
        findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Navigation: Achievements Screen
        findViewById<View>(R.id.clAchievement).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        // Logout functionality
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            sessionManager.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh dashboard data every time the screen becomes visible
        loadDashboardData()
    }

    /**
     * Fetches user data, expenses, and categories from the database to calculate
     * and display current financial statistics.
     */
    private fun loadDashboardData() {
        val userId = sessionManager.getUserId()
        // Redirect to login if session is invalid
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            val allExpenses = db.expenseDao().getAllExpenses(userId)
            val allCategories = db.categoryDao().getAllCategories(userId)
            
            // Calculate start of the current month to filter expenses
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            // Filter expenses and calculate totals
            val monthlyExpenses = allExpenses.filter { it.date >= startOfMonth }
            val totalSpent = monthlyExpenses.sumOf { it.amount }
            val income = user?.income ?: 0.0
            val budget = user?.monthlyBudget ?: 0.0
            val remaining = budget - totalSpent

            // Map categories to their respective spending amounts for progress visualization
            val categoryWithSpent = allCategories.map { category ->
                val spent = monthlyExpenses.filter { it.categoryId == category.id }.sumOf { it.amount }
                CategoryWithSpent(category, spent)
            }

            val categoryMap = allCategories.associate { it.id to it.name }

            // Basic logic for tracking achievement progress
            var unlockedCount = 0
            if (allExpenses.isNotEmpty()) unlockedCount++
            if (allExpenses.size >= 5) unlockedCount++
            if (allExpenses.size >= 10) unlockedCount++
            if (allExpenses.sumOf { it.amount } >= 5000) unlockedCount++

            // Update UI components on the main thread
            runOnUiThread {
                tvUsername.text = user?.username ?: "User"
                tvRemainingAmount.text = "R${String.format("%.2f", remaining)}"
                tvTotalExpenses.text = "R${String.format("%.2f", totalSpent)}"
                tvTotalIncome.text = "R${String.format("%.2f", income)}"
                tvAchievementCount.text = "$unlockedCount / 4 Unlocked"
                
                rvCategoryProgress.adapter = CategoryProgressAdapter(categoryWithSpent)
                rvRecentExpenses.adapter = ExpenseAdapter(allExpenses.take(5), categoryMap)
                
                // Show empty state message if no expenses exist
                if (allExpenses.isEmpty()) {
                    tvNoExpenses.visibility = View.VISIBLE
                    rvRecentExpenses.visibility = View.GONE
                } else {
                    tvNoExpenses.visibility = View.GONE
                    rvRecentExpenses.visibility = View.VISIBLE
                }
            }
        }
    }
}