package com.jnetai.workcalc.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jnetai.workcalc.R
import com.jnetai.workcalc.ui.employer.EmployersFragment
import com.jnetai.workcalc.ui.calendar.CalendarFragment
import com.jnetai.workcalc.ui.payslip.PayslipFragment
import com.jnetai.workcalc.ui.about.AboutFragment

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    private val employersFragment by lazy { EmployersFragment() }
    private val calendarFragment by lazy { CalendarFragment() }
    private val payslipFragment by lazy { PayslipFragment() }
    private val aboutFragment by lazy { AboutFragment() }

    private var activeFragment: Fragment = employersFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, aboutFragment, "about").hide(aboutFragment)
            add(R.id.fragment_container, payslipFragment, "payslip").hide(payslipFragment)
            add(R.id.fragment_container, calendarFragment, "calendar").hide(calendarFragment)
            add(R.id.fragment_container, employersFragment, "employers")
        }.commit()

        activeFragment = employersFragment

        bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_employers -> employersFragment
                R.id.nav_calendar -> calendarFragment
                R.id.nav_payslip -> payslipFragment
                R.id.nav_about -> aboutFragment
                else -> employersFragment
            }
            switchFragment(fragment)
            true
        }
    }

    private fun switchFragment(target: Fragment) {
        if (target == activeFragment) return
        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(target)
        }.commit()
        activeFragment = target
    }
}