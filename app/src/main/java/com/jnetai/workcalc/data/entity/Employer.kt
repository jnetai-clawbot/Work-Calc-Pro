package com.jnetai.workcalc.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employers")
data class Employer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hourlyRate: Double,
    val overtimeRate: Double,  // Multiplier (e.g., 1.5x)
    val overtimeThresholdHours: Double = 8.0,  // Hours before overtime kicks in per day
    val color: Int = 0xFF6200EE.toInt()  // Brand color for calendar dots
)