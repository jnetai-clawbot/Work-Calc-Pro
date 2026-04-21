package com.jnetai.workcalc.data.repository

import com.jnetai.workcalc.data.dao.EmployerDao
import com.jnetai.workcalc.data.entity.Employer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class EmployerRepository(private val dao: EmployerDao) {
    fun getAll(): Flow<List<Employer>> = dao.getAll()

    suspend fun getById(id: Long): Employer? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun insert(employer: Employer): Long = withContext(Dispatchers.IO) {
        dao.insert(employer)
    }

    suspend fun update(employer: Employer) = withContext(Dispatchers.IO) {
        dao.update(employer)
    }

    suspend fun delete(employer: Employer) = withContext(Dispatchers.IO) {
        dao.delete(employer)
    }
}