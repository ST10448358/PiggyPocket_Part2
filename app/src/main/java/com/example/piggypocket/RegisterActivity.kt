package com.example.piggypocket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        val etName = findViewById<EditText>(R.id.etName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val tvAlreadyRegistered = findViewById<TextView>(R.id.tvAlreadyRegistered)
        val btnBack = findViewById<View>(R.id.btnBack)

        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val email = etEmail.text.toString().trim()

            // Check if any field is empty
            if (name.isEmpty() || username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if the email ends with @gmail.com or @cloud.com
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
                !(email.endsWith("@gmail.com") || email.endsWith("@cloud.com"))) {
               Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
               return@setOnClickListener
            }

            // If validation passes, proceed with registration
            lifecycleScope.launch {
                try {
                    val newUser = User(username = username, password = password, email = email)
                    val userId = db.userDao().insert(newUser).toInt()
                    
                    // Add default categories for new user
                    val defaultCategories = listOf(
                        "Food 🍔", "Transport 🚗", "Shopping 🛍️", "Entertainment 🎮", "Bills 📄"
                    )
                    defaultCategories.forEach { catName ->
                        db.categoryDao().insert(Category(userId = userId, name = catName, budgetLimit = 0.0))
                    }
                    
                    runOnUiThread {
                        sessionManager.saveUserId(userId)
                        Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@RegisterActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        tvAlreadyRegistered.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}