package ru.plumsoftware.alarm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Context
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AlarmRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(navController: NavController, context: Context) {
    val repository = remember { AlarmRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var alarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.getAllAlarms().collectLatest { list ->
            alarms = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Будильник") },
                actions = {
                    IconButton(onClick = { navController.navigate("new_alarm") }) {
                        Text("+", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет будильников. Добавьте новый.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(alarms) { alarm ->
                    AlarmItem(alarm = alarm, onToggle = { enabled ->
                        val updated = alarm.copy(isEnabled = enabled)
                        coroutineScope.launch {
                            repository.update(updated)
                            if (enabled) {
                                AlarmManagerHelper.setAlarm(context, updated)
                            } else {
                                AlarmManagerHelper.cancelAlarm(context, updated)
                            }
                        }
                    }, onEdit = {
                        navController.navigate("edit_alarm/${alarm.id}")
                    }, onDelete = {
                        coroutineScope.launch {
                            repository.delete(alarm)
                            AlarmManagerHelper.cancelAlarm(context, alarm)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.8f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                fontSize = 48.sp,
                color = Color.White
            )
            Text(
                text = alarm.label,
                color = Color.Gray
            )
            Text(
                text = alarm.repeatDays.joinToString { dayToString(it) },
                color = Color.Gray
            )
        }
        Switch(
            checked = alarm.isEnabled,
            onCheckedChange = onToggle
        )
    }
    // For delete/edit, use gesture or long press, but for simplicity, assume edit on click
    // Add swipe to delete if needed, using Dismissable
}

fun dayToString(day: Int): String {
    return when (day) {
        0 -> "Вс"
        1 -> "Пн"
        2 -> "Вт"
        3 -> "Ср"
        4 -> "Чт"
        5 -> "Пт"
        6 -> "Сб"
        else -> ""
    }
}