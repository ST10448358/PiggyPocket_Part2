package com.example.piggypocket

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val amount: Double,
    val date: Long, // Store as timestamp
    val description: String,
    val categoryId: Int,
    val receiptPath: String? = null,
    val startTime: String? = null,
    val endTime: String? = null
)