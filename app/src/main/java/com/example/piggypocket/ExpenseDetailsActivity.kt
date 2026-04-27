package com.example.piggypocket

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseDetailsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_details)

        db = AppDatabase.getDatabase(this)

        val expenseId = intent.getIntExtra("EXPENSE_ID", -1)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        findViewById<Button>(R.id.btnDone).setOnClickListener {
            finish()
        }

        if (expenseId != -1) {
            loadExpenseDetails(expenseId)
        }
    }

    private fun loadExpenseDetails(id: Int) {
        lifecycleScope.launch {
            val expense = db.expenseDao().getExpenseById(id)
            if (expense != null) {
                val category = db.categoryDao().getCategoryById(expense.categoryId)
                
                runOnUiThread {
                    findViewById<TextView>(R.id.tvTotalAmount).text = "R ${String.format(Locale.getDefault(), "%.2f", expense.amount)}"
                    findViewById<TextView>(R.id.tvDetailCategory).text = category?.name ?: "Unknown"
                    findViewById<TextView>(R.id.tvDetailDescription).text = expense.description
                    
                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    findViewById<TextView>(R.id.tvDetailDate).text = sdf.format(Date(expense.date))

                    // Handle Start and End Time
                    val llTimeContainer = findViewById<View>(R.id.llTimeContainer)
                    val tvDetailTime = findViewById<TextView>(R.id.tvDetailTime)
                    
                    if (!expense.startTime.isNullOrEmpty() || !expense.endTime.isNullOrEmpty()) {
                        llTimeContainer.visibility = View.VISIBLE
                        val startTime = expense.startTime ?: "--:--"
                        val endTime = expense.endTime ?: "--:--"
                        tvDetailTime.text = "$startTime - $endTime"
                    } else {
                        llTimeContainer.visibility = View.GONE
                    }

                    // Handle receipt image
                    val llReceiptContainer = findViewById<View>(R.id.llReceiptContainer)
                    val ivReceipt = findViewById<ImageView>(R.id.ivReceipt)

                    if (!expense.receiptPath.isNullOrEmpty()) {
                        try {
                            val uri = Uri.parse(expense.receiptPath)
                            // For content URIs, we use the content resolver to get an input stream
                            if (uri.scheme == "content") {
                                val inputStream = contentResolver.openInputStream(uri)
                                val bitmap = BitmapFactory.decodeStream(inputStream)
                                if (bitmap != null) {
                                    ivReceipt.setImageBitmap(bitmap)
                                    llReceiptContainer.visibility = View.VISIBLE
                                } else {
                                    llReceiptContainer.visibility = View.GONE
                                }
                                inputStream?.close()
                            } else {
                                val imgFile = File(expense.receiptPath)
                                if (imgFile.exists()) {
                                    val myBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                                    ivReceipt.setImageBitmap(myBitmap)
                                    llReceiptContainer.visibility = View.VISIBLE
                                } else {
                                    // Try as a direct URI string if file path check fails
                                    ivReceipt.setImageURI(uri)
                                    llReceiptContainer.visibility = View.VISIBLE
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            llReceiptContainer.visibility = View.GONE
                        }
                    } else {
                        llReceiptContainer.visibility = View.GONE
                    }
                }
            }
        }
    }
}