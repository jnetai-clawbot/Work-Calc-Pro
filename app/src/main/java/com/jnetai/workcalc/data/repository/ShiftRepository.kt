package com.jnetai.workcalc.data.repository

import com.jnetai.workcalc.data.dao.ShiftDao
import com.jnetai.workcalc.data.entity.Shift
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShiftRepository(private val dao: ShiftDao) {
    fun getByEmployer(employerId: Long): Flow<List<Shift>> = dao.getByEmployer(employerId)
    fun getByDate(date: String): Flow<List<Shift>> = dao.getByDate(date)
    fun getByDateRange(startDate: String, endDate: String): Flow<List<Shift>> = dao.getByDateRange(startDate, endDate)
    fun getByEmployerAndDateRange(employerId: Long, startDate: String, endDate: String): Flow<List<Shift>> =
        dao.getByEmployerAndDateRange(employerId, startDate, endDate)

    suspend fun getById(id: Long): Shift? = withContext(Dispatchers.IO) { dao.getById(id) }

    suspend fun insert(shift: Shift): Long = withContext(Dispatchers.IO) { dao.insert(shift) }

    suspend fun update(shift: Shift) = withContext(Dispatchers.IO) { dao.update(shift) }

    suspend fun delete(shift: Shift) = withContext(Dispatchers.IO) { dao.delete(shift) }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }
}