package ru.plumsoftware.alarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ru.plumsoftware.alarm.AlarmReceiver
import java.util.concurrent.TimeUnit

//object AlarmManagerHelper {
//
//    fun setAlarm(context: Context, alarm: Alarm) {
//        val triggerTime = alarm.getNextAlarmTime()
//        val now = System.currentTimeMillis()
//
//        // Если время уже прошло — ставим на ближайший следующий день
//        val delayMillis = if (triggerTime > now) triggerTime - now else 0L
//
//        // Устанавливаем WorkManager с задержкой
//        val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
//            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
//            .addTag("alarm_${alarm.id}")
//            .setInputData(
//                Data.Builder()
//                    .putInt("alarm_id", alarm.id)
//                    .build()
//            )
//            .build()
//
//        WorkManager.getInstance(context).enqueueUniqueWork(
//            "alarm_${alarm.id}",
//            ExistingWorkPolicy.REPLACE,
//            workRequest
//        )
//    }
//
//    fun cancelAlarm(context: Context, alarm: Alarm) {
//        WorkManager.getInstance(context).cancelUniqueWork("alarm_${alarm.id}")
//    }
//}

object AlarmManagerHelper {
    fun setAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission not granted, cannot set exact alarm
                // Handle in UI, but for now, return or log
                return
            }
        }
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = alarm.getNextAlarmTime()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}