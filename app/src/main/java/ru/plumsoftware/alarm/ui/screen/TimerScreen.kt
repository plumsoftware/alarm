package ru.plumsoftware.alarm.ui.screen

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.plumsoftware.alarm.R
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmSecContainer
import ru.plumsoftware.alarm.ui.theme.alarmSecText
import ru.plumsoftware.alarm.ui.theme.primaryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import ru.plumsoftware.alarm.ui.theme.*

// --- START OF TIMER COMPONENTS ---
@SuppressLint("LocalContextResourcesRead")
@Composable
fun TimerScreen(
    selectedSound: Pair<String, Int>, // Текущий выбранный звук
    onSoundClick: () -> Unit          // Коллбек нажатия
) {
    // Состояния таймера
    var timeState by remember { mutableStateOf(TimerState.IDLE) }
    var totalTimeInMillis by remember { mutableLongStateOf(0L) }
    var remainingTimeInMillis by remember { mutableLongStateOf(0L) }

    val mediaPlayer = remember { MediaPlayer() }
    val context = LocalContext.current
    var currentVolume by remember { mutableFloatStateOf(1.0f) }

    // Значения пикеров
    var selectedHour by remember { mutableIntStateOf(0) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedSecond by remember { mutableIntStateOf(0) }

    // Функция для остановки звука
    fun stopAudio() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
    }

    // Очистка ресурсов при уходе с экрана
    DisposableEffect(Unit) {
        onDispose {
            stopAudio()
            mediaPlayer.release()
        }
    }

    // Анимация таймера и логика завершения
    LaunchedEffect(timeState, remainingTimeInMillis) {
        if (timeState == TimerState.RUNNING && remainingTimeInMillis > 0) {
            val startTime = System.currentTimeMillis()
            val startRemaining = remainingTimeInMillis

            while (timeState == TimerState.RUNNING && remainingTimeInMillis > 0) {
                delay(50L) // Обновляем чаще для плавности
                val elapsed = System.currentTimeMillis() - startTime
                remainingTimeInMillis = (startRemaining - elapsed).coerceAtLeast(0L)
            }

            // ТАЙМЕР ЗАВЕРШИЛСЯ
            if (remainingTimeInMillis == 0L && timeState == TimerState.RUNNING) {
                timeState = TimerState.IDLE

                // Запускаем звук
                try {
                    // Сначала сбрасываем, если что-то играло
                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                    mediaPlayer.reset()

                    val resourceId = selectedSound.second
                    val assetFd = context.resources.openRawResourceFd(resourceId)
                    mediaPlayer.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
                    assetFd.close()
                    mediaPlayer.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    mediaPlayer.isLooping = true // Зацикливаем звук таймера
                    mediaPlayer.prepare()
                    mediaPlayer.setVolume(currentVolume, currentVolume)
                    mediaPlayer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Контент (Пикеры или Круг)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (timeState == TimerState.IDLE && remainingTimeInMillis == 0L) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // --- ПИКЕРЫ ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimerWheelPicker(range = 0..23, label = "ч", onValueChange = { selectedHour = it })
                        TimerWheelPicker(range = 0..59, label = "мин", onValueChange = { selectedMinute = it })
                        TimerWheelPicker(range = 0..59, label = "с", onValueChange = { selectedSecond = it })
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- ПЛАШКА ВЫБОРА ЗВУКА (iOS Style) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(alarmCardColor)
                            .clickable { onSoundClick() }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "По окончании",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedSound.first,
                                style = MaterialTheme.typography.bodyMedium.copy(color = alarmGrayTextColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = alarmGrayTextColor
                            )
                        }
                    }
                }
            } else {
                // Круговой прогресс
                CircularTimerProgress(
                    totalTime = totalTimeInMillis,
                    remainingTime = remainingTimeInMillis
                )
            }
        }

        // Кнопки управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp)
                .padding(bottom = 80.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // КНОПКА ОТМЕНА
            TimerButton(
                text = "Отмена",
                backgroundColor = alarmCardColor,
                textColor = Color.White,
                onClick = {
                    timeState = TimerState.IDLE
                    remainingTimeInMillis = 0
                    stopAudio() // Останавливаем звук при отмене
                }
            )

            // КНОПКА СТАРТ / ПАУЗА
            val isRunning = timeState == TimerState.RUNNING
            TimerButton(
                text = if (isRunning) "Пауза" else if (remainingTimeInMillis > 0 && timeState == TimerState.PAUSED) "Дальше" else "Старт",
                backgroundColor = if (isRunning) Color(0xFF332800) else alarmSecContainer,
                textColor = if (isRunning) primaryColor else alarmSecText,
                onClick = {
                    stopAudio() // Если звук играет (таймер кончился), нажатие сюда тоже должно его остановить

                    when (timeState) {
                        TimerState.IDLE -> {
                            val total = (selectedHour * 3600 + selectedMinute * 60 + selectedSecond) * 1000L
                            if (total > 0) {
                                totalTimeInMillis = total
                                remainingTimeInMillis = total
                                timeState = TimerState.RUNNING
                            }
                        }
                        TimerState.RUNNING -> timeState = TimerState.PAUSED
                        TimerState.PAUSED -> timeState = TimerState.RUNNING
                    }
                }
            )
        }
    }
}

@Composable
fun TimerButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.dp, backgroundColor.copy(alpha = 0.5f), CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                fontSize = 18.sp
            )
        )
    }
}

@Composable
fun CircularTimerProgress(totalTime: Long, remainingTime: Long) {
    val progress = if (totalTime > 0) remainingTime.toFloat() / totalTime.toFloat() else 0f

    val hours = remainingTime / 1000 / 3600
    val minutes = (remainingTime / 1000 % 3600) / 60
    val seconds = remainingTime / 1000 % 60

    val timeText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    val endTime = Calendar.getInstance().apply {
        add(Calendar.MILLISECOND, remainingTime.toInt())
    }
    val endTimeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTime.time)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            drawCircle(
                color = alarmCardColor,
                style = Stroke(width = 12.dp.toPx())
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360 * progress,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    fontFeatureSettings = "tnum"
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.alarm),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = endTimeText,
                    style = TextStyle(color = Color.Gray, fontSize = 16.sp)
                )
            }
        }
    }
}

/**
 * БЕСКОНЕЧНЫЙ (LOOPING) TimerWheelPicker.
 * Решает проблему с выбором "00" и "пустотой" после последнего элемента,
 * так как список зациклен.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerWheelPicker(
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val itemCount = items.size

    // Создаем иллюзию бесконечности, используя очень большое число
    val infiniteCount = Int.MAX_VALUE

    // Стартуем с середины, чтобы можно было крутить и вверх, и вниз.
    // Вычисляем индекс в середине, который соответствует началу списка (значению 0)
    val middle = infiniteCount / 2
    val startIndex = middle - (middle % itemCount)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Размеры
    val itemHeight = 40.dp
    val visibleItemsCount = 5
    val pickerHeight = itemHeight * visibleItemsCount

    // Отступ для центрирования выбранного элемента
    val verticalPadding = (pickerHeight - itemHeight) / 2

    // Логика обновления значения (отслеживаем центральный элемент)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .map { (index, _) ->
                // Определяем индекс элемента, который сейчас в центре
                // Так как snapBehavior выравнивает по началу, мы используем layoutInfo
                val layoutInfo = listState.layoutInfo
                val viewportCenter = layoutInfo.viewportEndOffset / 2

                var closestIndex = index
                var minDiff = Int.MAX_VALUE

                for (visibleItem in layoutInfo.visibleItemsInfo) {
                    val itemCenter = visibleItem.offset + (visibleItem.size / 2)
                    val diff = kotlin.math.abs(viewportCenter - itemCenter)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIndex = visibleItem.index
                    }
                }
                closestIndex
            }
            .distinctUntilChanged()
            .collect { index ->
                // Преобразуем "бесконечный" индекс в реальное значение из range
                val realValue = items[index % itemCount]
                onValueChange(realValue)
            }
    }

    Box(
        modifier = Modifier
            .height(pickerHeight)
            .width(70.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Используем infiniteCount для бесконечной прокрутки
            items(infiniteCount) { index ->
                val actualItem = items[index % itemCount]

                // Визуальное выделение (прозрачность/размер)
                val isSelected by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                        if (visibleItem != null) {
                            val viewportCenter = layoutInfo.viewportEndOffset / 2
                            val itemCenter = visibleItem.offset + (visibleItem.size / 2)
                            kotlin.math.abs(viewportCenter - itemCenter) < (visibleItem.size / 2)
                        } else {
                            false
                        }
                    }
                }

                val color = if (isSelected) Color.White else Color.Gray.copy(alpha = 0.5f)
                val fontSize = if (isSelected) 24.sp else 20.sp
                val fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%02d", actualItem),
                            style = TextStyle(
                                color = color,
                                fontSize = fontSize,
                                fontWeight = fontWeight
                            )
                        )
                        if (isSelected) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    color = color,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class TimerState {
    IDLE, RUNNING, PAUSED
}
// --- END OF TIMER COMPONENTS ---