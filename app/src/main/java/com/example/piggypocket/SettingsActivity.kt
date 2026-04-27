package com.example.piggypocket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var etBudget: EditText
    private lateinit var tvOverviewBudget: TextView
    private lateinit var tvOverviewIncome: TextView
    private lateinit var tvRecommendedDaily: TextView
    private lateinit var etIncome: EditText
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        etBudget = findViewById(R.id.etBudget)
        tvOverviewBudget = findViewById(R.id.tvOverviewBudget)
        tvOverviewIncome = findViewById(R.id.tvOverviewIncome)
        tvRecommendedDaily = findViewById(R.id.tvRecommendedDaily)
        etIncome = findViewById(R.id.etIncome)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        loadUserData()

        findViewById<AppCompatButton>(R.id.btnSaveBudget).setOnClickListener {
            saveBudget()
        }

        findViewById<View>(R.id.cvGoals).setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java))
        }
    }

    private fun loadUserData() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            currentUser = db.userDao().getUserById(userId)
            currentUser?.let { user ->
                runOnUiThread {
                    etBudget.setText(user.monthlyBudget.toString())
                    etIncome.setText(user.income.toString())
                    updateOverview(user.monthlyBudget, user.income)
                }
            }
        }
    }

    private fun updateOverview(budget: Double, income: Double) {
        tvOverviewBudget.text = "R${String.format("%.2f", budget)}"
        tvOverviewIncome.text = "R${String.format("%.2f", income)}"
        val daily = budget / 30.0
        tvRecommendedDaily.text = "R${String.format("%.2f", daily)}"
    }

    // This function now saves BOTH budget and income
    private fun saveBudget() {

        val budgetText = etBudget.text.toString()
        val incomeText = etIncome.text.toString() // ✅ GET INCOME

        // Validate inputs
        if (budgetText.isEmpty() || incomeText.isEmpty()) {
            Toast.makeText(this, "Please enter both budget and income", Toast.LENGTH_SHORT).show()
            return
        }

        val budget = budgetText.toDoubleOrNull() ?: 0.0
        val income = incomeText.toDoubleOrNull() ?: 0.0 // ✅ CONVERT INCOME

        currentUser?.let { user ->

            // ✅ Update BOTH fields
            val updatedUser = user.copy(
                monthlyBudget = budget,
                income = income
            )

            lifecycleScope.launch {
                db.userDao().update(updatedUser)

                runOnUiThread {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Budget & Income updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }
            }
        }
    }
}