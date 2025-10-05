package ru.plumsoftware.alarm.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmGrayTextColor
import ru.plumsoftware.alarm.ui.theme.alarmSecContainer
import ru.plumsoftware.alarm.ui.theme.alarmSecPlaying
import ru.plumsoftware.alarm.ui.theme.alarmSecPlayingText
import ru.plumsoftware.alarm.ui.theme.alarmSecText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun formatStopwatchTime(milliseconds: Long): String {
    val totalMs = milliseconds
    val minutes = totalMs / 60_000
    val seconds = (totalMs % 60_000) / 1_000
    val centiseconds = (totalMs % 1_000) / 10 // 0..99
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, centiseconds)
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SecScreen() {
    var secTime by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    val del = 10L

    // Используем mutableStateListOf для корректного отслеживания изменений
    val laps = remember { mutableStateListOf<Pair<Int, Long>>() }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(del)
                secTime += del
            }
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, start = 6.dp, end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(space = 44.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatStopwatchTime(secTime),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 64.sp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(
                    8.dp,
                    alignment = Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Кнопка "Круг" / "Сброс"
                    Card(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = alarmCardColor,
                            contentColor = alarmGrayTextColor
                        ),
                        onClick = {
                            if (secTime > 0 && !isPlaying) {
                                // Сброс
                                secTime = 0
                                laps.clear()
                            } else if (isPlaying) {
                                // Добавить круг
                                val lapNumber = laps.size + 1
                                val lastLapTime = laps.lastOrNull()?.second ?: 0L
                                val currentLapTime = secTime - lastLapTime
                                laps.add(lapNumber to currentLapTime)
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (secTime > 0 && !isPlaying) "Сброс" else "Круг",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Кнопка "Старт" / "Стоп"
                    Card(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isPlaying) alarmSecContainer else alarmSecPlaying,
                            contentColor = if (!isPlaying) alarmSecText else alarmSecPlayingText
                        ),
                        onClick = {
                            isPlaying = !isPlaying
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (!isPlaying) "Старт" else "Стоп",
                                style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                HorizontalDivider(color = alarmGrayTextColor.copy(alpha = 0.2f), thickness = 2.dp)

                // Отображение кругов
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth(),
                    reverseLayout = true
                ) {
                    items(laps.size) { index ->
                        val (number, time) = laps[index]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(
                                space = 8.dp,
                                alignment = Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Круг $number")
                                Text(formatStopwatchTime(time))
                            }
                            HorizontalDivider(
                                color = alarmGrayTextColor.copy(alpha = 0.2f),
                                thickness = 2.dp
                            )
                            if (index == 0) {
                                Spacer(modifier = Modifier.height(120.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}