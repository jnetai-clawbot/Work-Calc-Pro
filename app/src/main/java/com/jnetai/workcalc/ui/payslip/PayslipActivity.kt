package com.jnetai.workcalc.ui.payslip

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jnetai.workcalc.R

class PayslipActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payslip)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
    }
}