package com.jnetai.workcalc.ui.shift

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.AppDatabase
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift
import com.jnetai.workcalc.data.repository.EmployerRepository
import com.jnetai.workcalc.data.repository.ShiftRepository
import com.jnetai.workcalc.util.UKCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShiftEditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_DATE = "shift_date"
        private const val EXTRA_SHIFT_ID = "shift_id"

        fun newIntent(context: Context, date: String): Intent {
            return Intent(context, ShiftEditActivity::class.java).apply {
                putExtra(EXTRA_DATE, date)
            }
        }

        fun newIntent(context: Context, shiftId: Long): Intent {
            return Intent(context, ShiftEditActivity::class.java).apply {
                putExtra(EXTRA_SHIFT_ID, shiftId)
            }
        }
    }

    private lateinit var shiftRepository: ShiftRepository
    private lateinit var employerRepository: EmployerRepository

    private var shiftId: Long = 0
    private var selectedDate = ""
    private var startHour = 9
    private var startMinute = 0
    private var endHour = 17
    private var endMinute = 0

    private var employers = listOf<Employer>()
    private var selectedEmployerId: Long = 0

    private lateinit var spinnerEmployer: Spinner
    private lateinit var inputStartTime: TextInputEditText
    private lateinit var inputEndTime: TextInputEditText
    private lateinit var inputBreak: TextInputEditText
    private lateinit var inputNotes: TextInputEditText
    private lateinit var textSummaryRegular: TextView
    private lateinit var textSummaryOvertime: TextView
    private lateinit var textSummaryPay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shift_edit)

        val db = AppDatabase.getInstance(this)
        shiftRepository = ShiftRepository(db.shiftDao())
        employerRepository = EmployerRepository(db.employerDao())

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        shiftId = intent.getLongExtra("shift_id", 0)
        selectedDate = intent.getStringExtra(EXTRA_DATE) ?: ""

        spinnerEmployer = findViewById(R.id.spinner_employer)
        inputStartTime = findViewById(R.id.input_start_time)
        inputEndTime = findViewById(R.id.input_end_time)
        inputBreak = findViewById(R.id.input_break)
        inputNotes = findViewById(R.id.input_notes)
        textSummaryRegular = findViewById(R.id.text_summary_regular)
        textSummaryOvertime = findViewById(R.id.text_summary_overtime)
        textSummaryPay = findViewById(R.id.text_summary_pay)

        inputStartTime.setOnClickListener { showTimePicker(true) }
        inputEndTime.setOnClickListener { showTimePicker(false) }

        updateTimeDisplay()

        loadEmployers()

        if (shiftId > 0) {
            toolbar.title = getString(R.string.edit_shift)
            loadShift()
        } else {
            toolbar.title = getString(R.string.add_shift)
        }

        findViewById<MaterialButton>(R.id.btn_save_shift).setOnClickListener { save() }

        inputBreak.setOnEditorActionListener { _, _, _ -> updateSummary(); false }
    }

    private fun loadEmployers() {
        lifecycleScope.launch {
            employerRepository.getAll().collect { list ->
                if (list.isNotEmpty()) {
                    employers = list
                    val names = listOf("Select Employer") + list.map { it.name }
                    val adapter = ArrayAdapter(
                        this@ShiftEditActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        names
                    )
                    spinnerEmployer.adapter = adapter
                    spinnerEmployer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            if (pos > 0 && pos <= employers.size) {
                                selectedEmployerId = employers[pos - 1].id
                            }
                            updateSummary()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            }
        }
    }

    private fun loadShift() {
        lifecycleScope.launch {
            val shift = withContext(Dispatchers.IO) { shiftRepository.getById(shiftId) } ?: return@launch
            selectedDate = shift.date
            val parts = shift.startTime.split(":")
            startHour = parts[0].toInt(); startMinute = parts[1].toInt()
            val endParts = shift.endTime.split(":")
            endHour = endParts[0].toInt(); endMinute = endParts[1].toInt()
            inputBreak.setText(shift.breakMinutes.toString())
            inputNotes.setText(shift.notes)
            selectedEmployerId = shift.employerId
            updateTimeDisplay()
            updateSummary()
        }
    }

    private fun showTimePicker(isStart: Boolean) {
        val h = if (isStart) startHour else endHour
        val m = if (isStart) startMinute else endMinute
        TimePickerDialog(this, { _, hour, minute ->
            if (isStart) {
                startHour = hour; startMinute = minute
            } else {
                endHour = hour; endMinute = minute
            }
            updateTimeDisplay()
            updateSummary()
        }, h, m, true).show()
    }

    private fun updateTimeDisplay() {
        inputStartTime.setText(String.format("%02d:%02d", startHour, startMinute))
        inputEndTime.setText(String.format("%02d:%02d", endHour, endMinute))
    }

    private fun updateSummary() {
        val employer = employers.find { it.id == selectedEmployerId } ?: return
        val tempShift = Shift(
            employerId = employer.id,
            date = selectedDate,
            startTime = String.format("%02d:%02d", startHour, startMinute),
            endTime = String.format("%02d:%02d", endHour, endMinute),
            breakMinutes = inputBreak.text?.toString()?.toIntOrNull() ?: 0
        )

        val regularHours = tempShift.getRegularHours(employer.overtimeThresholdHours)
        val overtimeHours = tempShift.getOvertimeHours(employer.overtimeThresholdHours)
        val pay = regularHours * employer.hourlyRate + overtimeHours * employer.hourlyRate * employer.overtimeRate

        textSummaryRegular.text = "Regular: ${"%.1f".format(regularHours)}h @ ${UKCalculator.formatCurrency(employer.hourlyRate)}/hr"
        textSummaryOvertime.text = "Overtime: ${"%.1f".format(overtimeHours)}h @ ${UKCalculator.formatCurrency(employer.hourlyRate * employer.overtimeRate)}/hr"
        textSummaryPay.text = "Total Pay: ${UKCalculator.formatCurrency(pay)}"
    }

    private fun save() {
        if (selectedEmployerId == 0L) {
            Toast.makeText(this, "Please select an employer", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "No date selected", Toast.LENGTH_SHORT).show()
            return
        }

        val startTime = String.format("%02d:%02d", startHour, startMinute)
        val endTime = String.format("%02d:%02d", endHour, endMinute)
        val breakMin = inputBreak.text?.toString()?.toIntOrNull() ?: 0
        val notes = inputNotes.text?.toString() ?: ""

        lifecycleScope.launch {
            if (shiftId > 0) {
                val existing = withContext(Dispatchers.IO) { shiftRepository.getById(shiftId) }
                if (existing != null) {
                    shiftRepository.update(existing.copy(
                        employerId = selectedEmployerId,
                        startTime = startTime,
                        endTime = endTime,
                        breakMinutes = breakMin,
                        notes = notes
                    ))
                }
            } else {
                shiftRepository.insert(Shift(
                    employerId = selectedEmployerId,
                    date = selectedDate,
                    startTime = startTime,
                    endTime = endTime,
                    breakMinutes = breakMin,
                    notes = notes
                ))
            }
            finish()
        }
    }
}