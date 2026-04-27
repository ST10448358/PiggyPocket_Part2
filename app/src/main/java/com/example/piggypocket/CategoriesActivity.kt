package com.example.piggypocket

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private lateinit var rvCategories: RecyclerView
    private var selectedIcon: String = "🛒"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        rvCategories = findViewById(R.id.rvCategories)
        rvCategories.layoutManager = LinearLayoutManager(this)

        loadCategories()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        findViewById<ImageButton>(R.id.btnAddCategory).setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun loadCategories() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories(userId)
            
            // If no categories, add default ones for this user
            if (categories.isEmpty()) {
                addDefaultCategories(userId)
                val newCategories = db.categoryDao().getAllCategories(userId)
                updateRecyclerView(newCategories)
            } else {
                updateRecyclerView(categories)
            }
        }
    }

    private suspend fun addDefaultCategories(userId: Int) {
        val defaults = listOf(
            Category(userId = userId, name = "Groceries 🛒", budgetLimit = 500.0),
            Category(userId = userId, name = "Transport 🚗", budgetLimit = 300.0),
            Category(userId = userId, name = "Entertainment 🎬", budgetLimit = 200.0),
            Category(userId = userId, name = "Utilities 💡", budgetLimit = 250.0),
            Category(userId = userId, name = "Shopping 🛍️", budgetLimit = 300.0)
        )
        defaults.forEach { db.categoryDao().insert(it) }
    }

    private fun updateRecyclerView(categories: List<Category>) {
        rvCategories.adapter = CategoryManageAdapter(
            categories,
            onEdit = { category -> showEditCategoryDialog(category) },
            onDelete = { category -> showDeleteConfirmationDialog(category) }
        )
    }

    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etBudget = dialogView.findViewById<EditText>(R.id.etCategoryBudget)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreate)
        val gridIcons = dialogView.findViewById<GridLayout>(R.id.gridIcons)
        val layoutColors = dialogView.findViewById<LinearLayout>(R.id.layoutColors)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup Icons
        for (i in 0 until gridIcons.childCount) {
            val child = gridIcons.getChildAt(i) as TextView
            child.isSelected = (child.tag == selectedIcon)
            child.setOnClickListener {
                for (j in 0 until gridIcons.childCount) {
                    gridIcons.getChildAt(j).isSelected = false
                }
                child.isSelected = true
                selectedIcon = child.tag.toString()
            }
        }

        // Setup Colors (Simplified logic, we store color if you have it in Category model, 
        // but for now we focus on the UI behavior from the picture)
        for (i in 0 until layoutColors.childCount) {
            val colorView = layoutColors.getChildAt(i)
            colorView.setOnClickListener {
                for (j in 0 until layoutColors.childCount) {
                    layoutColors.getChildAt(j).alpha = 0.5f
                }
                colorView.alpha = 1.0f
                // You could store this color in the category if needed
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnCreate.setOnClickListener {
            val name = etName.text.toString()
            val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty()) {
                saveCategoryToDb(name, budget)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveCategoryToDb(name: String, budget: Double) {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            db.categoryDao().insert(Category(userId = userId, name = "$name $selectedIcon", budgetLimit = budget))
            loadCategories()
            Toast.makeText(this@CategoriesActivity, "Category added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditCategoryDialog(category: Category) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etBudget = dialogView.findViewById<EditText>(R.id.etCategoryBudget)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreate)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)

        tvTitle.text = "Edit Category"
        // Try to separate emoji from name for editing
        val nameParts = category.name.split(" ")
        if (nameParts.size > 1) {
            etName.setText(nameParts.dropLast(1).joinToString(" "))
            selectedIcon = nameParts.last()
        } else {
            etName.setText(category.name)
        }
        
        etBudget.setText(category.budgetLimit.toString())
        btnCreate.text = "Update"

        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCreate.setOnClickListener {
            val newName = etName.text.toString()
            val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
            if (newName.isNotEmpty()) {
                updateCategory(category.copy(name = "$newName $selectedIcon", budgetLimit = newBudget))
                dialog.dismiss()
            }
        }

        dialogView.findViewById<View>(R.id.btnCancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun updateCategory(category: Category) {
        lifecycleScope.launch {
            db.categoryDao().update(category)
            loadCategories()
            Toast.makeText(this@CategoriesActivity, "Category updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            db.categoryDao().delete(category)
            loadCategories()
            Toast.makeText(this@CategoriesActivity, "Category deleted", Toast.LENGTH_SHORT).show()
        }
    }
}