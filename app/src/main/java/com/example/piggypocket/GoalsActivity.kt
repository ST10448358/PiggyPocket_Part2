package com.example.piggypocket

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Screen for setting and managing financial goals (Minimum and Maximum monthly targets).
 * These goals are used to track achievements and provide financial boundaries.
 */
class GoalsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        // Initialize database and session persistence
        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        // Initialize input fields
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)

        // Back button navigation
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Fetch and display existing goals
        loadUserGoals()

        // Handle the save button action
        findViewById<AppCompatButton>(R.id.btnSaveGoals).setOnClickListener {
            saveGoals()
        }
    }

    /**
     * Retrieves current user's goals from the database and populates the input fields.
     */
    private fun loadUserGoals() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let {
                // Formatting values to 2 decimal places for consistency
                etMinGoal.setText(String.format("%.2f", it.minMonthlyGoal))
                etMaxGoal.setText(String.format("%.2f", it.maxMonthlyGoal))
            }
        }
    }

    /**
     * Validates and saves the new goal values to the user's profile in the database.
     */
    private fun saveGoals() {
        val minGoal = etMinGoal.text.toString().toDoubleOrNull() ?: 0.0
        val maxGoal = etMaxGoal.text.toString().toDoubleOrNull() ?: 0.0

        // Validation: Ensure the minimum target doesn't exceed the maximum limit
        if (maxGoal > 0 && minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val user = db.userDao().getUserById(userId)
            user?.let {
                // Create a copy of the user with updated goal fields
                val updatedUser = it.copy(
                    minMonthlyGoal = minGoal,
                    maxMonthlyGoal = maxGoal
                )
                // Persist the changes to the database
                db.userDao().update(updatedUser)
                Toast.makeText(this@GoalsActivity, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                finish() // Close the screen and return to the previous one
            }
        }
    }
}