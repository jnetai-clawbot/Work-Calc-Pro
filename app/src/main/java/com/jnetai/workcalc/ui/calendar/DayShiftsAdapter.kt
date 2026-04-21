package com.jnetai.workcalc.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift

class DayShiftsAdapter(
    private val onDelete: (Shift) -> Unit
) : ListAdapter<Shift, DayShiftsAdapter.ViewHolder>(ShiftDiffCallback()) {

    private var employers = listOf<Employer>()

    fun updateEmployers(list: List<Employer>) {
        employers = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shift, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val shift = getItem(position)
        val employer = employers.find { it.id == shift.employerId }
        holder.bind(shift, employer, onDelete)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textEmployer: TextView = view.findViewById(R.id.text_shift_employer)
        private val textTime: TextView = view.findViewById(R.id.text_shift_time)
        private val colorDot: View = view.findViewById(R.id.shift_color_dot)
        private val btnDelete: ImageButton = view.findViewById(R.id.btn_delete_shift)

        fun bind(shift: Shift, employer: Employer?, onDelete: (Shift) -> Unit) {
            textEmployer.text = employer?.name ?: "Unknown Employer"
            val duration = shift.getDurationMinutes()
            val hours = duration / 60.0
            textTime.text = "${shift.startTime} - ${shift.endTime} (${String.format("%.1f", hours)}h)"
            colorDot.setBackgroundColor(employer?.color ?: 0xFF6200EE.toInt())
            btnDelete.setOnClickListener { onDelete(shift) }
        }
    }

    class ShiftDiffCallback : DiffUtil.ItemCallback<Shift>() {
        override fun areItemsTheSame(a: Shift, b: Shift) = a.id == b.id
        override fun areContentsTheSame(a: Shift, b: Shift) = a == b
    }
}