package com.jnetai.workcalc.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shifts",
    foreignKeys = [ForeignKey(
        entity = Employer::class,
        parentColumns = ["id"],
        childColumns = ["employerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("employerId"), Index("date")]
)
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employerId: Long,
    val date: String,           // yyyy-MM-dd
    val startTime: String,      // HH:mm
    val endTime: String,         // HH:mm
    val breakMinutes: Int = 0,
    val notes: String = ""
) {
    fun getDurationMinutes(): Int {
        val startParts = startTime.split(":")
        val endParts = endTime.split(":")
        val startMin = startParts[0].toInt() * 60 + startParts[1].toInt()
        var endMin = endParts[0].toInt() * 60 + endParts[1].toInt()
        if (endMin < startMin) endMin += 24 * 60  // overnight shift
        return (endMin - startMin) - breakMinutes
    }

    fun getRegularHours(threshold: Double = 8.0): Double {
        val totalHours = getDurationMinutes() / 60.0
        return minOf(totalHours, threshold)
    }

    fun getOvertimeHours(threshold: Double = 8.0): Double {
        val totalHours = getDurationMinutes() / 60.0
        return maxOf(0.0, totalHours - threshold)
    }
}