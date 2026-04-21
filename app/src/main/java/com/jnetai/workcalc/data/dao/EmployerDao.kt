package com.jnetai.workcalc.data.dao

import androidx.room.*
import com.jnetai.workcalc.data.entity.Employer
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployerDao {
    @Query("SELECT * FROM employers ORDER BY name")
    fun getAll(): Flow<List<Employer>>

    @Query("SELECT * FROM employers WHERE id = :id")
    suspend fun getById(id: Long): Employer?

    @Insert
    suspend fun insert(employer: Employer): Long

    @Update
    suspend fun update(employer: Employer)

    @Delete
    suspend fun delete(employer: Employer)
}