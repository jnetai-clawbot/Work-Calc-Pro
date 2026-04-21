package com.jnetai.workcalc.data.dao

import androidx.room.*
import com.jnetai.workcalc.data.entity.Shift
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE employerId = :employerId ORDER BY date, startTime")
    fun getByEmployer(employerId: Long): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE date = :date ORDER BY startTime")
    fun getByDate(date: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE date BETWEEN :startDate AND :endDate ORDER BY date, startTime")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE employerId = :employerId AND date BETWEEN :startDate AND :endDate ORDER BY date, startTime")
    fun getByEmployerAndDateRange(employerId: Long, startDate: String, endDate: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getById(id: Long): Shift?

    @Insert
    suspend fun insert(shift: Shift): Long

    @Update
    suspend fun update(shift: Shift)

    @Delete
    suspend fun delete(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteById(id: Long)
}