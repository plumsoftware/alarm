package ru.plumsoftware.alarm

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.plumsoftware.alarm.ui.screen.AlarmListScreen
import ru.plumsoftware.alarm.ui.screen.EditAlarmScreen
import ru.plumsoftware.alarm.ui.theme.AlarmTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createAlarmNotificationChannel()
        } else {
            showPermissionRationaleDialog()
        }
    }

    private fun createAlarmNotificationChannel() {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for alarm triggers"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Разрешение на уведомления необходимо")
            .setMessage("Чтобы будильник звонил, приложению нужно разрешение на отправку уведомлений. Пожалуйста, дайте разрешение в настройках.")
            .setPositiveButton("Настройки") { dialog, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Запрашиваем разрешение
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return // Не показываем контент, пока разрешение не получено
            } else {
                createAlarmNotificationChannel() // Уже есть разрешение — создаём канал
            }
        } else {
            // На Android < 13 — канал создаём без запроса
            createAlarmNotificationChannel()
        }

        enableEdgeToEdge()
        setContent {
            AlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "alarm_list") {
                        composable("alarm_list") {
                            AlarmListScreen(navController = navController, context = this@MainActivity)
                        }
                        composable("edit_alarm/{alarmId}") { backStackEntry ->
                            val alarmId = backStackEntry.arguments?.getString("alarmId")?.toInt() ?: -1
                            EditAlarmScreen(navController = navController, alarmId = alarmId, context = this@MainActivity)
                        }
                        composable("new_alarm") {
                            EditAlarmScreen(navController = navController, alarmId = -1, context = this@MainActivity)
                        }
                    }
                }
            }
        }
    }
}