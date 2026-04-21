package com.jnetai.workcalc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jnetai.workcalc.data.dao.EmployerDao
import com.jnetai.workcalc.data.dao.ShiftDao
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift

@Database(entities = [Employer::class, Shift::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employerDao(): EmployerDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workcalc.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}