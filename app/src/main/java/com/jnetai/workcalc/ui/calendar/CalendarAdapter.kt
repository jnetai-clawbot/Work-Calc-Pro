package com.jnetai.workcalc.ui.calendar

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.entity.Shift

data class CalendarDay(
    val dayNumber: Int,
    val date: String,
    val shifts: List<Shift>,
    val isCurrentMonth: Boolean = true,
    val isWeekend: Boolean = false
)

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    private var days = listOf<CalendarDay>()
    private var allShifts = listOf<Shift>()

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun updateShifts(shifts: List<Shift>) {
        allShifts = shifts
        // Re-map shifts to days
        val updated = days.map { day ->
            if (day.isCurrentMonth && day.dayNumber > 0) {
                day.copy(shifts = allShifts.filter { it.date == day.date })
            } else day
        }
        days = updated
        notifyDataSetChanged()
    }

    override fun getItemCount() = days.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = days[position]
        holder.bind(day, onDayClick)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textDay: TextView = view.findViewById(R.id.text_day_number)
        private val dotsContainer: LinearLayout = view.findViewById(R.id.dots_container)

        fun bind(day: CalendarDay, onClick: (CalendarDay) -> Unit) {
            if (day.dayNumber == 0) {
                textDay.text = ""
                dotsContainer.removeAllViews()
                itemView.setOnClickListener(null)
                itemView.alpha = 0f
                return
            }

            itemView.alpha = 1f
            textDay.text = day.dayNumber.toString()

            if (day.isWeekend) {
                textDay.setTextColor(Color.parseColor("#FF9800"))
            } else {
                textDay.setTextColor(itemView.context.getColor(R.color.md_theme_dark_onSurface))
            }

            // Draw shift dots
            dotsContainer.removeAllViews()
            val dp6 = (6 * itemView.context.resources.displayMetrics.density).toInt()
            val dp4 = (4 * itemView.context.resources.displayMetrics.density).toInt()

            for (shift in day.shifts.take(3)) {
                val dot = View(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(dp6, dp6).apply {
                        marginEnd = dp4 / 2
                    }
                    // Default colors by employer - using hash for variety
                    setBackgroundColor(getShiftColor(shift.employerId))
                }
                dotsContainer.addView(dot)
            }

            if (day.shifts.size > 3) {
                val more = TextView(itemView.context).apply {
                    text = "+${day.shifts.size - 3}"
                    textSize = 8f
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setTextColor(itemView.context.getColor(R.color.md_theme_dark_onSurfaceVariant))
                }
                dotsContainer.addView(more)
            }

            itemView.setOnClickListener { onClick(day) }
        }

        private fun getShiftColor(employerId: Long): Int {
            val colors = intArrayOf(
                0xFFBB86FC.toInt(),
                0xFF4CAF50.toInt(),
                0xFF2196F3.toInt(),
                0xFFFF9800.toInt(),
                0xFFF44336.toInt(),
                0xFF00BCD4.toInt()
            )
            return colors[(employerId % colors.size).toInt()]
        }
    }
}