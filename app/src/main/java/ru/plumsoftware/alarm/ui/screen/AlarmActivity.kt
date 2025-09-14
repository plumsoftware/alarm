package ru.plumsoftware.alarm.ui.screen

import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AppDatabase
import ru.plumsoftware.alarm.ui.theme.AlarmTheme
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.primaryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var alarmId = 0

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
            AlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    WallpapersBackground {
                        AlarmContent(
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
    }

    private suspend fun stopAlarm() {
        // 1. Останавливаем музыку
        mediaPlayer?.let { mp ->
            mp.stop()
            mp.release()
        }
        mediaPlayer = null

        // 2. Скрываем уведомление
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)

        // 3. Отменяем будильник (если он повторяющийся — не перезапланировать!)
        val db = AppDatabase.getDatabase(this)
        val alarm = db.alarmDao().getAlarmById(alarmId) ?: return
        AlarmManagerHelper.cancelAlarm(this, alarm)

        // 4. Завершаем активити
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).launch {
            stopAlarm()
        }
    }

    private fun playSound(context: Context, soundResId: Int) {
        if (soundResId == 0) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
            return
        }

        val mediaPlayer = MediaPlayer.create(context, soundResId)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer.start()
    }

    private fun snoozeAlarm(alarmId: Int) {
        val snoozeTime = System.currentTimeMillis() + 5 * 60 * 1000
        val cal = Calendar.getInstance()

        cal.timeInMillis = snoozeTime

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(this@AlarmActivity)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch

            val newAlarm = Alarm(
                id = alarmId,
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
                repeatDays = alarm.repeatDays,
                sound = alarm.sound,
                label = alarm.label
            )

            AlarmManagerHelper.setAlarm(this@AlarmActivity, newAlarm)

            Toast.makeText(this@AlarmActivity, "Будильник отложен на 5 минут", Toast.LENGTH_SHORT)
                .show()
        }
    }

    @Composable
    private fun WallpapersBackground(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
            content()
        }
    }

    @Composable
    private fun AlarmContent(
        onSnoozeClicked: () -> Unit,
        onCanselClicked: () -> Unit
    ) {
        val coroutine by remember { mutableStateOf(CoroutineScope(Dispatchers.IO)) }
        var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

        LaunchedEffect(key1 = Unit) {
            coroutine.launch {
                delay(1000L)
                currentTime = currentTime + 1000L
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
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
                    .padding(all = 18.dp)
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
                        .padding(16.dp)
                        .wrapContentSize()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "Отключить",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}