package ru.plumsoftware.alarm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.data.AppDatabase
import ru.plumsoftware.alarm.ui.screen.AlarmActivity
import kotlinx.coroutines.withContext
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import java.util.Calendar
import java.util.Date

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var mediaPlayer: MediaPlayer? = null
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        if (alarmId == -1) {
            Log.e("AlarmReceiver", "Invalid alarm_id: -1")
            return
        }

        Log.d(
            "AlarmReceiver",
            "⏰ Received alarm with ID: $alarmId at ${Date(System.currentTimeMillis())}"
        )

        playSoundAndVibrate(context, alarmId)

        rescheduleIfRepeating(context, alarmId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34 (Android 14)
            Log.d("AlarmReceiver", "📱 Android 14+ → using FullScreenIntent via Notification")
            showAlarmNotification(context, alarmId)
        } else {
            Log.d("AlarmReceiver", "📱 Android < 14 → launching Activity directly")
            launchAlarmActivity(context, alarmId)
        }
    }

    private fun launchAlarmActivity(context: Context, alarmId: Int) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            // ⚠️ КРИТИЧЕСКИ ВАЖНЫЕ ФЛАГИ:
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

            putExtra("alarm_id", alarmId)
        }

        // Запускаем активити из BroadcastReceiver — это безопасно с FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Log.d("AlarmReceiver", "✅ Launched AlarmActivity for alarm $alarmId directly")
    }

    private fun playSoundAndVibrate(context: Context, alarmId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch

            // Освобождаем предыдущий плеер
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }

            val soundResId = alarm.sound

            // Создаём MediaPlayer
            mediaPlayer = if (soundResId != 0) {
                MediaPlayer.create(context, soundResId)
            } else {
                MediaPlayer.create(
                    context,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                )
            }

            mediaPlayer?.let { mp ->
                mp.isLooping = true

                // Получаем AudioManager для получения макс. громкости
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val targetVolume = (maxVolume * 0.5f).toInt() // 50% от максимума

                // Устанавливаем начальную громкость на 0
                mp.setVolume(0f, 0f)

                withContext(Dispatchers.Main) {
                    mp.start()
                }

                // Плавно увеличиваем громкость до 50% за 5 секунд
                val steps = 50 // 50 шагов = каждые 100 мс
                val delayMs = 100L
                var currentStep = 0

                while (currentStep < steps && mp.isPlaying) {
                    delay(delayMs)
                    currentStep++
                    val volume =
                        (targetVolume * currentStep / steps).toFloat() / maxVolume.toFloat()
                    mp.setVolume(volume, volume)
                }

                // Убедимся, что в конце громкость точно 50%
                if (mp.isPlaying) {
                    val finalVolume = targetVolume.toFloat() / maxVolume.toFloat()
                    mp.setVolume(finalVolume, finalVolume)
                }
            }

            // Вибрация — без изменений
            withContext(Dispatchers.Main) {
                val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        1000,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }
    }

    private fun showAlarmNotification(context: Context, alarmId: Int) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("alarm_id", alarmId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("⏰ Будильник")
            .setContentText("Нажмите для отключения")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Нажмите для отключения"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarmId, notificationBuilder.build())
    }

    private fun rescheduleIfRepeating(context: Context, alarmId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: run {
                Log.e("AlarmReceiver", "Alarm with ID $alarmId not found in DB")
                return@launch
            }

            Log.d(
                "AlarmReceiver",
                "Checking if alarm $alarmId is repeating. repeatDays = ${alarm.repeatDays}"
            )

            if (alarm.repeatDays.contains(0)) {
                Log.d("AlarmReceiver", "Alarm $alarmId is marked as NON-repeating (contains 0)")
                return@launch
            }

            if (alarm.repeatDays.isEmpty()) {
                Log.d("AlarmReceiver", "Alarm $alarmId has empty repeatDays → NON-repeating")
                return@launch
            }

            val nextAlarmTime = alarm.getNextAlarmTime()
            val nextDate = Date(nextAlarmTime)
            val cal = Calendar.getInstance().apply { timeInMillis = nextAlarmTime }

            Log.d("AlarmReceiver", "🔁 Rescheduling repeating alarm $alarmId to: $nextDate")

            val updatedAlarm = alarm.copy(
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE)
            )

            db.alarmDao().update(updatedAlarm)
            Log.d(
                "AlarmReceiver",
                "✅ Updated alarm in DB: ${updatedAlarm.hour}:${updatedAlarm.minute}"
            )

            AlarmManagerHelper.setAlarm(context, updatedAlarm)
            Log.d("AlarmReceiver", "✅ Set new alarm in AlarmManager for ID: ${updatedAlarm.id}")
        }
    }
}