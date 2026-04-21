package com.jnetai.workcalc.ui.employer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.AppDatabase
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.repository.EmployerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmployerEditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_EMPLOYER_ID = "employer_id"

        fun newIntent(context: Context, employerId: Long = 0): Intent {
            return Intent(context, EmployerEditActivity::class.java).apply {
                putExtra(EXTRA_EMPLOYER_ID, employerId)
            }
        }
    }

    private lateinit var repository: EmployerRepository
    private var employerId: Long = 0
    private var selectedColor: Int = 0xFF6200EE.toInt()

    private val colors = intArrayOf(
        0xFF6200EE.toInt(),  // Purple (default)
        0xFF4CAF50.toInt(),  // Green
        0xFF2196F3.toInt(),  // Blue
        0xFFFF9800.toInt(),  // Orange
        0xFFF44336.toInt(),  // Red
        0xFF00BCD4.toInt()   // Cyan
    )

    private lateinit var inputName: TextInputEditText
    private lateinit var inputHourlyRate: TextInputEditText
    private lateinit var inputOvertimeRate: TextInputEditText
    private lateinit var inputOvertimeThreshold: TextInputEditText
    private lateinit var colorPicker: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_employer_edit)

        repository = EmployerRepository(AppDatabase.getInstance(this).employerDao())
        employerId = intent.getLongExtra(EXTRA_EMPLOYER_ID, 0)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        inputName = findViewById(R.id.input_employer_name)
        inputHourlyRate = findViewById(R.id.input_hourly_rate)
        inputOvertimeRate = findViewById(R.id.input_overtime_rate)
        inputOvertimeThreshold = findViewById(R.id.input_overtime_threshold)
        colorPicker = findViewById(R.id.color_picker)

        setupColorPicker()

        if (employerId > 0) {
            loadEmployer()
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
                .title = getString(R.string.edit_employer)
        } else {
            inputOvertimeThreshold.setText("8.0")
            inputOvertimeRate.setText("1.5")
            findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
                .title = getString(R.string.add_employer)
        }

        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener { save() }
    }

    private fun setupColorPicker() {
        val dp32 = (32 * resources.displayMetrics.density).toInt()
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        colorPicker.removeAllViews()

        for (color in colors) {
            val circle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp32, dp32).apply {
                    marginEnd = dp8
                }
                setBackgroundColor(color and 0xFFFFFFFF.toInt())
                setOnClickListener {
                    selectedColor = color
                    updateColorSelection()
                }
            }
            colorPicker.addView(circle)
        }
        updateColorSelection()
    }

    private fun updateColorSelection() {
        for (i in 0 until colorPicker.childCount) {
            val child = colorPicker.getChildAt(i)
            val dp4 = (4 * resources.displayMetrics.density).toInt()
            if (colors[i] == selectedColor) {
                child.setPadding(dp4, dp4, dp4, dp4)
                child.setBackgroundResource(android.R.drawable.btn_star_big_on)
                child.setBackgroundColor(colors[i] and 0xFFFFFFFF.toInt())
            } else {
                child.setPadding(0, 0, 0, 0)
                child.background = null
                child.setBackgroundColor(colors[i] and 0xFFFFFFFF.toInt())
            }
        }
    }

    private fun loadEmployer() {
        lifecycleScope.launch {
            val employer = withContext(Dispatchers.IO) { repository.getById(employerId) } ?: return@launch
            inputName.setText(employer.name)
            inputHourlyRate.setText(employer.hourlyRate.toString())
            inputOvertimeRate.setText(employer.overtimeRate.toString())
            inputOvertimeThreshold.setText(employer.overtimeThresholdHours.toString())
            selectedColor = employer.color
            updateColorSelection()
        }
    }

    private fun save() {
        val name = inputName.text?.toString()?.trim() ?: ""
        val hourlyRate = inputHourlyRate.text?.toString()?.toDoubleOrNull() ?: 0.0
        val overtimeRate = inputOvertimeRate.text?.toString()?.toDoubleOrNull() ?: 1.5
        val threshold = inputOvertimeThreshold.text?.toString()?.toDoubleOrNull() ?: 8.0

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter employer name", Toast.LENGTH_SHORT).show()
            return
        }
        if (hourlyRate <= 0) {
            Toast.makeText(this, "Please enter a valid hourly rate", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            if (employerId > 0) {
                val existing = withContext(Dispatchers.IO) { repository.getById(employerId) }
                if (existing != null) {
                    repository.update(existing.copy(
                        name = name,
                        hourlyRate = hourlyRate,
                        overtimeRate = overtimeRate,
                        overtimeThresholdHours = threshold,
                        color = selectedColor
                    ))
                }
            } else {
                repository.insert(Employer(
                    name = name,
                    hourlyRate = hourlyRate,
                    overtimeRate = overtimeRate,
                    overtimeThresholdHours = threshold,
                    color = selectedColor
                ))
            }
            finish()
        }
    }
}