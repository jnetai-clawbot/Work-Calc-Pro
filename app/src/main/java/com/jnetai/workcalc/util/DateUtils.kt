package com.jnetai.workcalc.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val TIME_FORMAT = "HH:mm"

    fun formatDate(date: Date): String = SimpleDateFormat(DATE_FORMAT, Locale.UK).format(date)
    fun parseDate(str: String): Date = SimpleDateFormat(DATE_FORMAT, Locale.UK).parse(str)!!
    fun formatTime(hour: Int, minute: Int): String = String.format(Locale.UK, "%02d:%02d", hour, minute)

    fun getMonthStart(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0) }
        return formatDate(cal.time)
    }

    fun getMonthEnd(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return formatDate(cal.time)
    }

    fun getWeekStart(date: String): String {
        val cal = Calendar.getInstance().apply {
            time = parseDate(date)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return formatDate(cal.time)
    }

    fun getWeekEnd(date: String): String {
        val cal = Calendar.getInstance().apply {
            time = parseDate(date)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        return formatDate(cal.time)
    }

    fun getMonthName(month: Int): String {
        val cal = Calendar.getInstance().apply { set(Calendar.MONTH, month) }
        return SimpleDateFormat("MMMM", Locale.UK).format(cal.time)
    }

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance().apply { set(year, month, 1) }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dayOfWeek(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    fun isWeekend(year: Int, month: Int, day: Int): Boolean {
        val dow = dayOfWeek(year, month, day)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }
}