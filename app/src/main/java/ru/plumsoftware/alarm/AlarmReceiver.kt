package ru.plumsoftware.alarm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.data.AppDatabase
import ru.plumsoftware.alarm.ui.screen.AlarmActivity
import androidx.core.content.edit
import kotlinx.coroutines.withContext

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) return

        // Воспроизводим звук и вибрацию
        playSoundAndVibrate(context, alarmId)

        // Показываем уведомление
        showAlarmNotification(context, alarmId)
    }

    private fun playSoundAndVibrate(context: Context, alarmId: Int) {
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch

            val soundResId = alarm.sound

            // Звук
            val mediaPlayer = if (soundResId != 0) MediaPlayer.create(
                context,
                soundResId
            ) else
                MediaPlayer.create(
                    context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                )
            mediaPlayer.isLooping = true
            withContext(Dispatchers.Main) {
                mediaPlayer.start()
            }

            // Сохраняем ссылку для остановки позже
            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.edit { putInt("current_alarm_id", alarmId) }
            prefs.edit { putBoolean("is_playing", true) }

            // Вибрация
            withContext(Dispatchers.Main) {
                val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: Int) {
        val notificationBuilder = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Добавь иконку в res/drawable
            .setContentTitle("⏰ Будильник")
            .setContentText("Нажмите для отключения")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Нажмите для отключения"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false) // Не удалять автоматически
            .setOngoing(true)     // Постоянное уведомление
            .setOnlyAlertOnce(true)
            .setShowWhen(true)

        // Создаём PendingIntent, который откроет AlarmActivity
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alarm_id", alarmId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.setContentIntent(pendingIntent)

        // Отправляем уведомление
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId, notificationBuilder.build())
    }
}