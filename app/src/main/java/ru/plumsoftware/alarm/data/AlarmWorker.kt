package ru.plumsoftware.alarm.data

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.plumsoftware.alarm.ui.screen.AlarmActivity

class AlarmWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val alarmId = inputData.getInt("alarm_id", -1)
        if (alarmId == -1) return Result.failure()

        val context = applicationContext
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra("alarm_id", alarmId)
        }

        context.startActivity(intent)

//        playSoundAndVibrate(context, alarmId)

        return Result.success()
    }

//    private fun playSoundAndVibrate(context: Context, alarmId: Int) {
//        val db = AppDatabase.getDatabase(context)
//        val alarm = db.alarmDao().getAlarmById(alarmId) ?: return
//
//        val soundResId = alarm.sound
//
//        // Воспроизведение звука
//        if (soundResId != 0) {
//            val mediaPlayer = MediaPlayer.create(context, soundResId)
//            mediaPlayer.setOnCompletionListener { mp ->
//                mp.release()
//            }
//            mediaPlayer.start()
//        } else {
//            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
//            val ringtone = RingtoneManager.getRingtone(context, uri)
//            ringtone.play()
//        }
//
//        // Вибрация
//        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
//        vibrator?.vibrate(
//            VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
//        )
//    }
}