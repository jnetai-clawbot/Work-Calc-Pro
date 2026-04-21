package com.jnetai.workcalc.ui.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.AppDatabase
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift
import com.jnetai.workcalc.data.repository.EmployerRepository
import com.jnetai.workcalc.data.repository.ShiftRepository
import com.jnetai.workcalc.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var textMonthYear: TextView
    private lateinit var recyclerCalendar: RecyclerView
    private lateinit var recyclerDayShifts: RecyclerView
    private lateinit var textSelectedDate: TextView
    private lateinit var textNoShifts: TextView
    private lateinit var fabAddShift: FloatingActionButton

    private lateinit var shiftRepository: ShiftRepository
    private lateinit var employerRepository: EmployerRepository

    private var currentYear = 0
    private var currentMonth = 0
    private var selectedDate = ""
    private var allShifts = listOf<Shift>()
    private var allEmployers = listOf<Employer>()
    private var dayShifts = listOf<Shift>()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var dayShiftsAdapter: DayShiftsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        shiftRepository = ShiftRepository(db.shiftDao())
        employerRepository = EmployerRepository(db.employerDao())

        textMonthYear = view.findViewById(R.id.text_month_year)
        recyclerCalendar = view.findViewById(R.id.recycler_calendar)
        recyclerDayShifts = view.findViewById(R.id.recycler_day_shifts)
        textSelectedDate = view.findViewById(R.id.text_selected_date)
        textNoShifts = view.findViewById(R.id.text_no_shifts)
        fabAddShift = view.findViewById(R.id.fab_add_shift)

        val cal = Calendar.getInstance()
        currentYear = cal.get(Calendar.YEAR)
        currentMonth = cal.get(Calendar.MONTH)
        selectedDate = DateUtils.formatDate(cal.time)

        calendarAdapter = CalendarAdapter { day -> onDayClicked(day) }
        recyclerCalendar.layoutManager = GridLayoutManager(requireContext(), 7)
        recyclerCalendar.adapter = calendarAdapter

        dayShiftsAdapter = DayShiftsAdapter(
            onDelete = { shift -> confirmDeleteShift(shift) }
        )
        recyclerDayShifts.layoutManager = LinearLayoutManager(requireContext())
        recyclerDayShifts.adapter = dayShiftsAdapter

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener {
            currentMonth--
            if (currentMonth < 0) { currentMonth = 11; currentYear-- }
            updateCalendar()
        }

        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener {
            currentMonth++
            if (currentMonth > 11) { currentMonth = 0; currentYear++ }
            updateCalendar()
        }

        fabAddShift.setOnClickListener {
            startActivity(com.jnetai.workcalc.ui.shift.ShiftEditActivity.newIntent(requireContext(), selectedDate))
        }

        observeData()
        updateCalendar()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val startDate = DateUtils.getMonthStart(currentYear, currentMonth)
            val endDate = DateUtils.getMonthEnd(currentYear, currentMonth)
            shiftRepository.getByDateRange(startDate, endDate).collectLatest { shifts ->
                allShifts = shifts
                calendarAdapter.updateShifts(shifts)
                updateDayShifts()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            employerRepository.getAll().collectLatest { employers ->
                allEmployers = employers
                dayShiftsAdapter.updateEmployers(employers)
            }
        }
    }

    private fun updateCalendar() {
        textMonthYear.text = "${DateUtils.getMonthName(currentMonth)} $currentYear"

        // Generate calendar cells
        val days = mutableListOf<CalendarDay>()
        val firstDayOfWeek = DateUtils.dayOfWeek(currentYear, currentMonth, 1)
        // Convert to Monday-based index (0=Mon, 6=Sun)
        val offset = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0; Calendar.TUESDAY -> 1; Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3; Calendar.FRIDAY -> 4; Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6; else -> 0
        }

        for (i in 0 until offset) {
            days.add(CalendarDay(0, "", emptyList(), false))
        }

        val totalDays = DateUtils.daysInMonth(currentYear, currentMonth)
        for (day in 1..totalDays) {
            val date = String.format("%04d-%02d-%02d", currentYear, currentMonth + 1, day)
            val shiftsForDay = allShifts.filter { it.date == date }
            val isWeekend = DateUtils.isWeekend(currentYear, currentMonth, day)
            days.add(CalendarDay(day, date, shiftsForDay, true, isWeekend))
        }

        calendarAdapter.updateDays(days)

        // Re-observe for new month
        viewLifecycleOwner.lifecycleScope.launch {
            val startDate = DateUtils.getMonthStart(currentYear, currentMonth)
            val endDate = DateUtils.getMonthEnd(currentYear, currentMonth)
            shiftRepository.getByDateRange(startDate, endDate).collectLatest { shifts ->
                allShifts = shifts
                calendarAdapter.updateShifts(shifts)
                updateDayShifts()
            }
        }
    }

    private fun onDayClicked(day: CalendarDay) {
        if (day.dayNumber == 0) return
        selectedDate = day.date
        updateDayShifts()
    }

    private fun updateDayShifts() {
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.UK)
        val date = DateUtils.parseDate(selectedDate)
        textSelectedDate.text = sdf.format(date)

        dayShifts = allShifts.filter { it.date == selectedDate }
        dayShiftsAdapter.submitList(dayShifts)

        textNoShifts.visibility = if (dayShifts.isEmpty()) View.VISIBLE else View.GONE
        recyclerDayShifts.visibility = if (dayShifts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDeleteShift(shift: Shift) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    shiftRepository.delete(shift)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}