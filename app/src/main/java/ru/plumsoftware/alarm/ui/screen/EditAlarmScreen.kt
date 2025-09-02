package ru.plumsoftware.alarm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AlarmRepository
import ru.plumsoftware.alarm.ui.IOSStyleTimePicker
import android.provider.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmScreen(navController: NavController, alarmId: Int, context: Context) {
    val repository = remember { AlarmRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var alarm by remember { mutableStateOf(Alarm(hour = 7, minute = 0)) }
    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager }

    if (alarmId != -1) {
        LaunchedEffect(alarmId) {
            // Load alarm from DB, but for simplicity, assume new or fetch
            // In real, fetch from repo
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* After returning from settings, can check again if needed */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == -1) "Новый будильник" else "Изменить будильник") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Отмена")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val canSchedule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            alarmManager.canScheduleExactAlarms()
                        } else true
                        if (canSchedule) {
                            coroutineScope.launch {
                                val savedAlarm = if (alarmId == -1) {
                                    repository.insert(alarm)
                                    alarm  // Note: id is auto-generated, but for simplicity, assume we refetch or update
                                } else {
                                    repository.update(alarm.copy(id = alarmId))
                                    alarm.copy(id = alarmId)
                                }
                                AlarmManagerHelper.setAlarm(context, savedAlarm)
                                navController.popBackStack()
                            }
                        } else {
                            // Request permission
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            launcher.launch(intent)
                        }
                    }) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Custom Time Picker to mimic iOS wheel
            IOSStyleTimePicker(
                hour = alarm.hour,
                minute = alarm.minute,
                onTimeChange = { h, m ->
                    alarm = alarm.copy(hour = h, minute = m)
                }
            )

            // List of options
            LazyColumn {
                item {
                    ListItem("Повторение") { /* Navigate to repeat selection */ }
                }
                item {
                    ListItem("Метка") { /* Text field */ }
                }
                item {
                    ListItem("Звук") { /* Sound picker */ }
                }
                item {
                    Row {
                        Text("Отложить")
                        Switch(
                            checked = alarm.snoozeEnabled,
                            onCheckedChange = { alarm = alarm.copy(snoozeEnabled = it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListItem(title: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(title)
    }
}