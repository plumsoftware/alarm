package ru.plumsoftware.alarm.ui.screen

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.plumsoftware.alarm.AlarmReceiver
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AppDatabase
import ru.plumsoftware.alarm.ui.theme.AlarmTheme
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmScreen
import ru.plumsoftware.alarm.ui.theme.primaryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var alarmId = 0
    private var isSnoozing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        alarmId = intent.getIntExtra("alarm_id", 0)
        if (alarmId == 0) {
            finish()
            return
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enableEdgeToEdge()
        setContent {
            val coroutine = rememberCoroutineScope()
            var name by remember { mutableStateOf("Будильник") }
            LaunchedEffect(Unit) {
                coroutine.launch {
                    val db = AppDatabase.getDatabase(this@AlarmActivity)
                    val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
                    withContext(Dispatchers.Main) {
                        name = alarm.label
                    }
                }
            }
            AlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    AlarmContent(
                        name = name,
                        onSnoozeClicked = {
                            snoozeAlarm(alarmId)
                            finish()
                        },
                        onCanselClicked = {
                            CoroutineScope(Dispatchers.IO).launch {
                                stopAlarm()
                            }.invokeOnCompletion {
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    private suspend fun stopAlarm() {
        // 1. Останавливаем музыку
        AlarmReceiver.mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.stop()
            }
            mp.release()
        }
        AlarmReceiver.mediaPlayer = null

        // 2. Скрываем уведомление
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        // 3. Отменяем будильник ТОЛЬКО если он НЕ повторяющийся И мы НЕ откладываем
        val db = AppDatabase.getDatabase(this)
        val alarm = db.alarmDao().getAlarmById(alarmId) ?: return

        if (!isSnoozing && (alarm.repeatDays.contains(0) || alarm.repeatDays.isEmpty())) {
            AlarmManagerHelper.cancelAlarm(this, alarm)
            Log.d("AlarmActivity", "🔕 Manual cancel of alarm $alarmId")
        }

        if (alarm.repeatDays.isEmpty() || alarm.repeatDays == listOf(0)) {
            db.alarmDao().update(alarm.copy(isEnabled = false))
        }

        // 4. Сбрасываем флаг
        isSnoozing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isSnoozing) {
            CoroutineScope(Dispatchers.IO).launch {
                stopAlarm()
            }
        }
    }

    private suspend fun playSoundAndVibrate(context: Context, alarmId: Int) {
        val db = AppDatabase.getDatabase(context)
        val alarm = db.alarmDao().getAlarmById(alarmId) ?: return

        val soundResId = alarm.sound

        // Звук
        mediaPlayer = if (soundResId != 0) {
            MediaPlayer.create(context, soundResId)
        } else {
            MediaPlayer.create(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        }

        mediaPlayer?.let { mp ->
            mp.isLooping = true
            withContext(Dispatchers.Main) {
                mp.start()
            }
        }

        // Вибрация
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

    private fun snoozeAlarm(alarmId: Int) {
        isSnoozing = true // ← УСТАНАВЛИВАЕМ ФЛАГ

        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
        val cal = Calendar.getInstance().apply { timeInMillis = snoozeTime }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@AlarmActivity)
            val originalAlarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch

            val isRepeating =
                originalAlarm.repeatDays.isNotEmpty() && !originalAlarm.repeatDays.contains(0)

            if (isRepeating) {
                val tempSnoozeId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                val tempSnoozeAlarm = Alarm(
                    id = tempSnoozeId,
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = cal.get(Calendar.MINUTE),
                    repeatDays = listOf(0),
                    sound = originalAlarm.sound,
                    label = originalAlarm.label,
                    isEnabled = true,
                    snoozeEnabled = originalAlarm.snoozeEnabled
                )

                db.alarmDao().insert(tempSnoozeAlarm)
                AlarmManagerHelper.setAlarm(this@AlarmActivity, tempSnoozeAlarm)

                Log.d(
                    "AlarmActivity",
                    "🕒 Snoozed REPEATING alarm $alarmId → new TEMP alarm $tempSnoozeId at ${
                        Date(snoozeTime)
                    }"
                )
            } else {
                AlarmManagerHelper.cancelAlarm(this@AlarmActivity, originalAlarm)

                val newAlarm = Alarm(
                    id = alarmId,
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = cal.get(Calendar.MINUTE),
                    repeatDays = listOf(0),
                    sound = originalAlarm.sound,
                    label = originalAlarm.label,
                    isEnabled = true,
                    snoozeEnabled = originalAlarm.snoozeEnabled
                )

                db.alarmDao().update(newAlarm)
                AlarmManagerHelper.setAlarm(this@AlarmActivity, newAlarm)

                Log.d(
                    "AlarmActivity",
                    "🕒 Snoozed NON-repeating alarm $alarmId → reset to ${Date(snoozeTime)}"
                )
            }

            // Останавливаем звук и уведомление
            AlarmReceiver.mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
            AlarmReceiver.mediaPlayer = null

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(alarmId)

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@AlarmActivity,
                    "Будильник отложен на 5 минут",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    @Composable
    private fun AlarmContent(
        name: String,
        onSnoozeClicked: () -> Unit,
        onCanselClicked: () -> Unit
    ) {
        var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

        LaunchedEffect(key1 = Unit) {
            while (true) {
                delay(1000L)
                currentTime += 1000L
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(alarmScreen)
                .clickable(true) { /* блокируем клики снаружи */ }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime)),
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onSnoozeClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize()
                ) {
                    Text(
                        text = "Отложить",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(all = 24.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = onCanselClicked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = alarmCardColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .padding(38.dp)
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "Отключить",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}