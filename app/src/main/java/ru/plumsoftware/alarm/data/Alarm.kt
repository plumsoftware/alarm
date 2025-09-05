package ru.plumsoftware.alarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val repeatDays: List<Int> = emptyList(),  // 0=Sun, 1=Mon, ..., 6=Sat
    val label: String = "",
    val sound: String = "default",  // Placeholder for sound URI or name
    val snoozeEnabled: Boolean = false
) {
    fun getNextAlarmTime(): Long {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            // Handle repeatDays logic: find next day if not today
            if (repeatDays.isNotEmpty()) {
                while (!repeatDays.contains(get(Calendar.DAY_OF_WEEK) - 1)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return alarmTime.timeInMillis
    }
}