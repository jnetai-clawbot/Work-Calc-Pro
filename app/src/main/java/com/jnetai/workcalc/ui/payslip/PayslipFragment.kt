package com.jnetai.workcalc.ui.payslip

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.AppDatabase
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift
import com.jnetai.workcalc.data.repository.EmployerRepository
import com.jnetai.workcalc.data.repository.ShiftRepository
import com.jnetai.workcalc.util.DateUtils
import com.jnetai.workcalc.util.PayslipGenerator
import com.jnetai.workcalc.util.UKCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class PayslipFragment : Fragment() {

    private lateinit var chipPeriod: ChipGroup
    private lateinit var spinnerEmployer: Spinner
    private lateinit var textPeriodLabel: TextView
    private lateinit var textGross: TextView
    private lateinit var textIncomeTax: TextView
    private lateinit var textNI: TextView
    private lateinit var textNetPay: TextView
    private lateinit var switchHousing: SwitchMaterial
    private lateinit var textUCDeduction: TextView
    private lateinit var textTakeHome: TextView
    private lateinit var textEffectiveRate: TextView
    private lateinit var textTotalRegular: TextView
    private lateinit var textTotalOvertime: TextView
    private lateinit var textTotalShifts: TextView
    private lateinit var btnExportPdf: MaterialButton
    private lateinit var btnShareText: MaterialButton

    private lateinit var shiftRepository: ShiftRepository
    private lateinit var employerRepository: EmployerRepository

    private var employers = listOf<Employer>()
    private var selectedEmployerId: Long = 0  // 0 = all
    private var isWeekly = true
    private var currentOffset = 0

    // Current period's data
    private var currentShifts = listOf<Shift>()
    private var currentEmployer: Employer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_payslip, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        shiftRepository = ShiftRepository(db.shiftDao())
        employerRepository = EmployerRepository(db.employerDao())

        chipPeriod = view.findViewById(R.id.chip_period)
        spinnerEmployer = view.findViewById(R.id.spinner_payslip_employer)
        textPeriodLabel = view.findViewById(R.id.text_period_label)
        textGross = view.findViewById(R.id.text_gross)
        textIncomeTax = view.findViewById(R.id.text_income_tax)
        textNI = view.findViewById(R.id.text_ni)
        textNetPay = view.findViewById(R.id.text_net_pay)
        switchHousing = view.findViewById(R.id.switch_has_housing)
        textUCDeduction = view.findViewById(R.id.text_uc_deduction)
        textTakeHome = view.findViewById(R.id.text_take_home)
        textEffectiveRate = view.findViewById(R.id.text_effective_rate)
        textTotalRegular = view.findViewById(R.id.text_total_regular)
        textTotalOvertime = view.findViewById(R.id.text_total_overtime)
        textTotalShifts = view.findViewById(R.id.text_total_shifts)
        btnExportPdf = view.findViewById(R.id.btn_export_pdf)
        btnShareText = view.findViewById(R.id.btn_share_text)

        chipPeriod.setOnCheckedStateChangeListener { _, _ ->
            isWeekly = view.findViewById<Chip>(R.id.chip_weekly).isChecked
            currentOffset = 0
            updatePeriod()
        }

        view.findViewById<ImageButton>(R.id.btn_prev_period).setOnClickListener {
            currentOffset--
            updatePeriod()
        }

        view.findViewById<ImageButton>(R.id.btn_next_period).setOnClickListener {
            currentOffset++
            updatePeriod()
        }

        switchHousing.setOnCheckedChangeListener { _, _ -> recalculate() }

        loadEmployers()

        btnExportPdf.setOnClickListener { exportPdf() }
        btnShareText.setOnClickListener { shareText() }

        updatePeriod()
    }

    private fun loadEmployers() {
        viewLifecycleOwner.lifecycleScope.launch {
            employerRepository.getAll().collectLatest { list ->
                employers = list
                val names = listOf("All Employers") + list.map { it.name }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                spinnerEmployer.adapter = adapter
                spinnerEmployer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedEmployerId = if (pos == 0) 0L else employers[pos - 1].id
                        currentEmployer = if (pos > 0) employers[pos - 1] else null
                        recalculate()
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun updatePeriod() {
        val cal = Calendar.getInstance()
        if (isWeekly) {
            cal.add(Calendar.WEEK_OF_YEAR, currentOffset)
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = DateUtils.formatDate(cal.time)
            cal.add(Calendar.DAY_OF_MONTH, 6)
            val weekEnd = DateUtils.formatDate(cal.time)
            textPeriodLabel.text = "Week: $weekStart to $weekEnd"
            loadShiftsForPeriod(weekStart, weekEnd)
        } else {
            cal.add(Calendar.MONTH, currentOffset)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val monthStart = DateUtils.getMonthStart(year, month)
            val monthEnd = DateUtils.getMonthEnd(year, month)
            textPeriodLabel.text = "${DateUtils.getMonthName(month)} $year"
            loadShiftsForPeriod(monthStart, monthEnd)
        }
    }

    private fun loadShiftsForPeriod(startDate: String, endDate: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val flow = if (selectedEmployerId > 0)
                shiftRepository.getByEmployerAndDateRange(selectedEmployerId, startDate, endDate)
            else
                shiftRepository.getByDateRange(startDate, endDate)

            flow.collectLatest { shifts ->
                currentShifts = shifts
                recalculate()
            }
        }
    }

    private fun recalculate() {
        val hasHousing = switchHousing.isChecked

        // Calculate total pay from shifts
        var totalRegular = 0.0
        var totalOvertime = 0.0
        var totalGross = 0.0

        for (shift in currentShifts) {
            val employer = employers.find { it.id == shift.employerId } ?: continue
            val reg = shift.getRegularHours(employer.overtimeThresholdHours)
            val ot = shift.getOvertimeHours(employer.overtimeThresholdHours)
            totalRegular += reg
            totalOvertime += ot
            totalGross += reg * employer.hourlyRate + ot * employer.hourlyRate * employer.overtimeRate
        }

        // Convert to monthly for tax calc
        val monthlyGross = if (isWeekly) totalGross * 52.0 / 12.0 else totalGross

        val breakdown = UKCalculator.completeMonthlyBreakdown(monthlyGross, hasHousing)

        // For display, if weekly, show weekly equivalents
        val displayGross = if (isWeekly) totalGross else totalGross
        val displayTax = if (isWeekly) breakdown.incomeTax * 12.0 / 52.0 else breakdown.incomeTax
        val displayNI = if (isWeekly) breakdown.nationalInsurance * 12.0 / 52.0 else breakdown.nationalInsurance
        val displayNet = if (isWeekly) breakdown.netPay * 12.0 / 52.0 else breakdown.netPay
        val displayUC = if (isWeekly) breakdown.ucDeduction * 12.0 / 52.0 else breakdown.ucDeduction
        val displayTakeHome = if (isWeekly) breakdown.finalTakeHome * 12.0 / 52.0 else breakdown.finalTakeHome

        val fmt = UKCalculator::formatCurrency

        textGross.text = "Gross Pay: ${fmt(displayGross)}"
        textIncomeTax.text = "Income Tax: -${fmt(displayTax)}"
        textNI.text = "National Insurance: -${fmt(displayNI)}"
        textNetPay.text = "Net Pay: ${fmt(displayNet)}"
        textUCDeduction.text = "UC Deduction (55%): -${fmt(displayUC)}"
        textTakeHome.text = "Take Home: ${fmt(displayTakeHome)}"
        textEffectiveRate.text = "Effective rate: ${"%.1f".format(breakdown.effectiveRate)}%"

        textTotalRegular.text = "Regular Hours: ${"%.1f".format(totalRegular)}"
        textTotalOvertime.text = "Overtime Hours: ${"%.1f".format(totalOvertime)}"
        textTotalShifts.text = "${currentShifts.size} shifts"
    }

    private fun exportPdf() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = buildPayslipData() ?: return@launch
            val file = withContext(Dispatchers.IO) {
                PayslipGenerator.generatePdf(requireContext(), data)
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open PDF"))
        }
    }

    private fun shareText() {
        viewLifecycleOwner.lifecycleScope.launch {
            val data = buildPayslipData() ?: return@launch
            val text = PayslipGenerator.generateText(data)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "Share Payslip"))
        }
    }

    private fun buildPayslipData(): PayslipGenerator.PayslipData? {
        if (currentShifts.isEmpty()) {
            Toast.makeText(requireContext(), "No shifts for this period", Toast.LENGTH_SHORT).show()
            return null
        }

        var totalRegular = 0.0
        var totalOvertime = 0.0
        var totalGross = 0.0

        for (shift in currentShifts) {
            val employer = employers.find { it.id == shift.employerId } ?: continue
            val reg = shift.getRegularHours(employer.overtimeThresholdHours)
            val ot = shift.getOvertimeHours(employer.overtimeThresholdHours)
            totalRegular += reg
            totalOvertime += ot
            totalGross += reg * employer.hourlyRate + ot * employer.hourlyRate * employer.overtimeRate
        }

        val monthlyGross = if (isWeekly) totalGross * 52.0 / 12.0 else totalGross
        val breakdown = UKCalculator.completeMonthlyBreakdown(monthlyGross, switchHousing.isChecked)

        val employer = currentEmployer ?: Employer(name = "All Employers", hourlyRate = 0.0, overtimeRate = 1.5)

        return PayslipGenerator.PayslipData(
            employer = employer,
            period = textPeriodLabel.text.toString(),
            shifts = currentShifts,
            totalRegularHours = totalRegular,
            totalOvertimeHours = totalOvertime,
            grossPay = if (isWeekly) totalGross else totalGross,
            incomeTax = if (isWeekly) breakdown.incomeTax * 12.0 / 52.0 else breakdown.incomeTax,
            nationalInsurance = if (isWeekly) breakdown.nationalInsurance * 12.0 / 52.0 else breakdown.nationalInsurance,
            netPay = if (isWeekly) breakdown.netPay * 12.0 / 52.0 else breakdown.netPay,
            ucDeduction = if (isWeekly) breakdown.ucDeduction * 12.0 / 52.0 else breakdown.ucDeduction,
            takeHomePay = if (isWeekly) breakdown.finalTakeHome * 12.0 / 52.0 else breakdown.finalTakeHome
        )
    }
}