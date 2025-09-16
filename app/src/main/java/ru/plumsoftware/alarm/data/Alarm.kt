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
    val repeatDays: List<Int> = emptyList(),  // 0=не повторять, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    val label: String = "",
    val sound: Int = 0,  // Placeholder for sound URI or name
    val snoozeEnabled: Boolean = false
) {
    fun getNextAlarmTime(): Long {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Если время уже прошло сегодня — переносим на завтра
        if (alarmTime.timeInMillis <= now.timeInMillis) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Специальное правило: если в repeatDays есть 0 — не повторять, возвращаем ближайшее срабатывание
        if (repeatDays.contains(0)) {
            return alarmTime.timeInMillis
        }

        // Если repeatDays пуст — тоже не повторяем (одноразовый будильник)
        if (repeatDays.isEmpty()) {
            return alarmTime.timeInMillis
        }

        // Иначе — ищем ближайший день из repeatDays (1=Mon, ..., 7=Sun)
        // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon, ..., 7=Sat → нужно преобразовать в твою систему
        while (true) {
            val calendarDay = alarmTime.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
            val mappedDay = when (calendarDay) {
                1 -> 7   // Воскресенье → 7
                2 -> 1   // Понедельник → 1
                3 -> 2   // Вторник → 2
                4 -> 3   // Среда → 3
                5 -> 4   // Четверг → 4
                6 -> 5   // Пятница → 5
                7 -> 6   // Суббота → 6
                else -> throw IllegalStateException("Unexpected day of week: $calendarDay")
            }

            if (repeatDays.contains(mappedDay)) {
                break
            }
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return alarmTime.timeInMillis
    }
}