package com.example.piggypocket

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var ivReceipt: ImageView
    private lateinit var tvUploadPrompt: TextView
    private lateinit var etAmount: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var spinnerCategory: Spinner
    
    private var photoUri: Uri? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private lateinit var db: AppDatabase
    private lateinit var sessionManager: SessionManager
    private var categoryList: List<Category> = emptyList()

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            displaySelectedImage(photoUri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val savedUri = copyUriToInternalStorage(it)
                photoUri = savedUri
                displaySelectedImage(savedUri)
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
            val outputStream = file.outputStream()
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        db = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)

        ivReceipt = findViewById(R.id.ivReceipt)
        tvUploadPrompt = findViewById(R.id.tvUploadPrompt)
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        val llUploadReceipt = findViewById<LinearLayout>(R.id.llUploadReceipt)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        etDate.setOnClickListener {
            showDatePicker()
        }

        etStartTime.setOnClickListener {
            showTimePicker(etStartTime)
        }

        etEndTime.setOnClickListener {
            showTimePicker(etEndTime)
        }

        llUploadReceipt.setOnClickListener {
            showImagePickerOptions()
        }

        findViewById<Button>(R.id.btnAddExpense).setOnClickListener {
            saveExpense()
        }

        loadCategories()
    }

    private fun loadCategories() {
        val userId = sessionManager.getUserId()
        lifecycleScope.launch {
            categoryList = db.categoryDao().getAllCategories(userId)
            if (categoryList.isEmpty()) {
                // Insert default categories if none exist
                val defaults = listOf("Groceries 🛒", "Transport 🚗", "Entertainment 🎬", "Utilities 💡", "Shopping 🛍️")
                defaults.forEach { db.categoryDao().insert(Category(userId = userId, name = it)) }
                categoryList = db.categoryDao().getAllCategories(userId)
            }
            
            val adapter = ArrayAdapter(this@AddExpenseActivity, android.R.layout.simple_spinner_item, categoryList.map { it.name })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            runOnUiThread {
                spinnerCategory.adapter = adapter
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                val format = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                etDate.setText(format.format(selectedDate.time))
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val timeCalendar = Calendar.getInstance()
                timeCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                timeCalendar.set(Calendar.MINUTE, selectedMinute)
                val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
                editText.setText(format.format(timeCalendar.time))
            },
            hour,
            minute,
            false
        )
        timePickerDialog.show()
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload Receipt")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen()
                1 -> pickImageLauncher.launch("image/*")
                else -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun displaySelectedImage(uri: Uri?) {
        uri?.let {
            ivReceipt.setImageURI(it)
            val params = ivReceipt.layoutParams as LinearLayout.LayoutParams
            params.width = LinearLayout.LayoutParams.MATCH_PARENT
            params.height = LinearLayout.LayoutParams.MATCH_PARENT
            ivReceipt.layoutParams = params
            ivReceipt.scaleType = ImageView.ScaleType.CENTER_CROP
            tvUploadPrompt.visibility = View.GONE
            ivReceipt.imageTintList = null
        }
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString()
        val description = etDescription.text.toString()
        val userId = sessionManager.getUserId()
        
        if (amountStr.isEmpty() || description.isEmpty() || categoryList.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val categoryId = categoryList[spinnerCategory.selectedItemPosition].id

        lifecycleScope.launch {
            val expense = Expense(
                userId = userId,
                amount = amount,
                date = selectedDate.timeInMillis,
                description = description,
                categoryId = categoryId,
                receiptPath = photoUri?.toString(),
                startTime = etStartTime.text.toString().takeIf { it.isNotEmpty() },
                endTime = etEndTime.text.toString().takeIf { it.isNotEmpty() }
            )
            val expenseId = db.expenseDao().insert(expense)
            
            runOnUiThread {
                Toast.makeText(this@AddExpenseActivity, "Expense added successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@AddExpenseActivity, ExpenseDetailsActivity::class.java)
                intent.putExtra("EXPENSE_ID", expenseId.toInt())
                startActivity(intent)
                finish()
            }
        }
    }

    private fun openCamera() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            null
        }
        photoFile?.also {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
            photoUri = uri
            takePhotoLauncher.launch(uri)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
}