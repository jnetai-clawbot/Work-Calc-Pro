package com.jnetai.workcalc.ui.employer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jnetai.workcalc.R
import com.jnetai.workcalc.data.AppDatabase
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.repository.EmployerRepository
import com.jnetai.workcalc.util.UKCalculator
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmployersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: EmployerAdapter
    private lateinit var repository: EmployerRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_employers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = EmployerRepository(AppDatabase.getInstance(requireContext()).employerDao())
        recyclerView = view.findViewById(R.id.recycler_employers)
        textEmpty = view.findViewById(R.id.text_empty)
        fabAdd = view.findViewById(R.id.fab_add_employer)

        adapter = EmployerAdapter(
            onClick = { employer -> editEmployer(employer) },
            onDelete = { employer -> confirmDelete(employer) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener { addEmployer() }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAll().collectLatest { employers ->
                adapter.submitList(employers)
                textEmpty.visibility = if (employers.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (employers.isEmpty()) View.GONE else View.VISIBLE
            }
        }
    }

    private fun addEmployer() {
        startActivity(EmployerEditActivity.newIntent(requireContext()))
    }

    private fun editEmployer(employer: Employer) {
        startActivity(EmployerEditActivity.newIntent(requireContext(), employer.id))
    }

    private fun confirmDelete(employer: Employer) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_delete)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.delete(employer)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }
}