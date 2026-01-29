package ru.plumsoftware.alarm.ui.screen

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.plumsoftware.alarm.R
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmSecContainer
import ru.plumsoftware.alarm.ui.theme.alarmSecText
import ru.plumsoftware.alarm.ui.theme.primaryColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- START OF TIMER COMPONENTS ---

@Composable
fun TimerScreen() {
    // Состояния таймера
    var timeState by remember { mutableStateOf(TimerState.IDLE) }
    var totalTimeInMillis by remember { mutableLongStateOf(0L) }
    var remainingTimeInMillis by remember { mutableLongStateOf(0L) }

    // Значения пикеров
    var selectedHour by remember { mutableIntStateOf(0) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var selectedSecond by remember { mutableIntStateOf(0) }

    // Анимация таймера
    LaunchedEffect(timeState, remainingTimeInMillis) {
        if (timeState == TimerState.RUNNING && remainingTimeInMillis > 0) {
            val startTime = System.currentTimeMillis()
            val startRemaining = remainingTimeInMillis

            while (timeState == TimerState.RUNNING && remainingTimeInMillis > 0) {
                delay(50L) // Обновляем чаще для плавности
                val elapsed = System.currentTimeMillis() - startTime
                remainingTimeInMillis = (startRemaining - elapsed).coerceAtLeast(0L)
            }

            if (remainingTimeInMillis == 0L && timeState == TimerState.RUNNING) {
                timeState = TimerState.IDLE
                // Здесь можно добавить звук окончания таймера
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp), // Отступ сверху как в оригинале
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Контент меняется в зависимости от состояния
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (timeState == TimerState.IDLE) {
                // Пикер времени (Часы, Минуты, Секунды)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerWheelPicker(
                        range = 0..23,
                        label = "ч",
                        onValueChange = { selectedHour = it }
                    )
                    TimerWheelPicker(
                        range = 0..59,
                        label = "мин",
                        onValueChange = { selectedMinute = it }
                    )
                    TimerWheelPicker(
                        range = 0..59,
                        label = "с",
                        onValueChange = { selectedSecond = it }
                    )
                }
            } else {
                // Круговой прогресс
                CircularTimerProgress(
                    totalTime = totalTimeInMillis,
                    remainingTime = remainingTimeInMillis
                )
            }
        }

        // Кнопки управления (Низ экрана)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 40.dp) // Отступ от низа
                .padding(bottom = 80.dp), // Место под BottomBar
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка Отмена / Сброс
            TimerButton(
                text = "Отмена",
                backgroundColor = alarmCardColor,
                textColor = Color.White, // Серый в оригинале, но белый читабельнее на темном
                onClick = {
                    timeState = TimerState.IDLE
                    remainingTimeInMillis = 0
                }
            )

            // Кнопка Старт / Пауза / Дальше
            val isRunning = timeState == TimerState.RUNNING
            TimerButton(
                text = if (isRunning) "Пауза" else if (remainingTimeInMillis > 0 && timeState == TimerState.PAUSED) "Дальше" else "Старт",
                backgroundColor = if (isRunning) Color(0xFF332800) else alarmSecContainer, // Желтоватый фон для паузы
                textColor = if (isRunning) primaryColor else alarmSecText, // Оранжевый текст для паузы
                onClick = {
                    when (timeState) {
                        TimerState.IDLE -> {
                            val total = (selectedHour * 3600 + selectedMinute * 60 + selectedSecond) * 1000L
                            if (total > 0) {
                                totalTimeInMillis = total
                                remainingTimeInMillis = total
                                timeState = TimerState.RUNNING
                            }
                        }
                        TimerState.RUNNING -> {
                            timeState = TimerState.PAUSED
                        }
                        TimerState.PAUSED -> {
                            timeState = TimerState.RUNNING
                        }
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
            .size(80.dp) // Размер кнопок как в iOS
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Внешняя обводка (для стиля кнопки "Старт" в iOS есть двойное кольцо, упростим до одного)
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

    // Форматирование времени
    val hours = remainingTime / 1000 / 3600
    val minutes = (remainingTime / 1000 % 3600) / 60
    val seconds = remainingTime / 1000 % 60

    val timeText = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    // Колокольчик (окончание)
    val endTime = Calendar.getInstance().apply {
        add(Calendar.MILLISECOND, remainingTime.toInt())
    }
    val endTimeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(endTime.time)

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            // Фон круга (серый)
            drawCircle(
                color = alarmCardColor,
                style = Stroke(width = 12.dp.toPx())
            )
            // Прогресс (оранжевый)
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
                    fontFeatureSettings = "tnum" // Моноширинные цифры
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.alarm), // Используем ваш ресурс или Icons.Rounded.Notifications
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

// Упрощенная реализация колеса прокрутки в стиле iOS
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerWheelPicker(
    range: IntRange,
    label: String,
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val listState = rememberLazyListState()

    // Высота видимой области и одного элемента
    val itemHeight = 40.dp
    val visibleItemsCount = 5
    val pickerHeight = itemHeight * visibleItemsCount

    // ВАЖНО: Отступы, чтобы первый и последний элемент вставали РОВНО по центру
    // (Высота контейнера - Высота элемента) / 2
    val verticalPadding = (pickerHeight - itemHeight) / 2

    // "Магнит": список будет останавливаться ровно на элементах, а не между ними
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Логика определения выбранного элемента
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val viewportCenter = layoutInfo.viewportEndOffset / 2
                var closestItemIndex = -1
                var minDistance = Int.MAX_VALUE

                // Ищем элемент, чей центр ближе всего к центру контейнера
                for (item in layoutInfo.visibleItemsInfo) {
                    val itemCenter = item.offset + (item.size / 2)
                    val distance = kotlin.math.abs(viewportCenter - itemCenter)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestItemIndex = item.index
                    }
                }

                // Передаем значение, если нашли валидный индекс
                if (closestItemIndex in items.indices) {
                    onValueChange(items[closestItemIndex])
                }
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
            flingBehavior = flingBehavior, // Включаем "прилипание"
            contentPadding = PaddingValues(vertical = verticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(items) { item ->
                // Вычисляем прозрачность и размер для эффекта "барабана"
                val isSelected by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == items.indexOf(item) }
                        if (visibleItem != null) {
                            val viewportCenter = layoutInfo.viewportEndOffset / 2
                            val itemCenter = visibleItem.offset + (visibleItem.size / 2)
                            // Если элемент в пределах половины своей высоты от центра — он выбран
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
                            text = String.format("%02d", item),
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