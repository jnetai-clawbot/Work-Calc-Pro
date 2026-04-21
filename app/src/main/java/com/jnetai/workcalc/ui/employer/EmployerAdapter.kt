package com.jnetai.workcalc.ui.employer

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
import com.jnetai.workcalc.util.UKCalculator

class EmployerAdapter(
    private val onClick: (Employer) -> Unit,
    private val onDelete: (Employer) -> Unit
) : ListAdapter<Employer, EmployerAdapter.ViewHolder>(EmployerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_employer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val employer = getItem(position)
        holder.bind(employer)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText: TextView = view.findViewById(R.id.text_employer_name)
        private val ratesText: TextView = view.findViewById(R.id.text_rates)
        private val colorIndicator: View = view.findViewById(R.id.color_indicator)
        private val deleteBtn: ImageButton = view.findViewById(R.id.btn_delete_employer)

        fun bind(employer: Employer) {
            nameText.text = employer.name
            ratesText.text = "${UKCalculator.formatCurrency(employer.hourlyRate)}/hr | OT: ${employer.overtimeRate}x after ${employer.overtimeThresholdHours}h"
            colorIndicator.setBackgroundColor(employer.color)
            deleteBtn.setOnClickListener { onDelete(employer) }
            itemView.setOnClickListener { onClick(employer) }
        }
    }

    class EmployerDiffCallback : DiffUtil.ItemCallback<Employer>() {
        override fun areItemsTheSame(a: Employer, b: Employer) = a.id == b.id
        override fun areContentsTheSame(a: Employer, b: Employer) = a == b
    }
}