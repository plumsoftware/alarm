package ru.plumsoftware.alarm

import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AppDatabase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Vibrator
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(/* name = */ "alarm_id", /* defaultValue = */ -1)
        if (alarmId == -1) return

        // Play sound
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone.play()

        // Vibrate
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))

        // Reschedule if repeating
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
            if (alarm.repeatDays.isNotEmpty()) {
                AlarmManagerHelper.setAlarm(context, alarm)
            }
        }

        // TODO: Show full-screen alarm activity or notification for snooze/dismiss
    }
}