package ru.plumsoftware.alarm.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {
    private val alarmDao = AppDatabase.getDatabase(context).alarmDao()

    fun getAllAlarms(): Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun insert(alarm: Alarm) = alarmDao.insert(alarm)

    suspend fun update(alarm: Alarm) = alarmDao.update(alarm)

    suspend fun delete(alarm: Alarm) = alarmDao.delete(alarm)
}