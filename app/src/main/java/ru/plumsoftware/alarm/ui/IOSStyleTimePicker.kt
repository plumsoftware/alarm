package ru.plumsoftware.alarm.ui

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun IOSStyleTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit
) {
    var currentHour by remember { mutableStateOf(hour) }
    var currentMinute by remember { mutableStateOf(minute) }
    var hourOffset by remember { mutableStateOf(0f) }
    var minuteOffset by remember { mutableStateOf(0f) }

    Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        // Hour wheel
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            (0..23).forEach { h ->
                Text(
                    text = String.format("%02d", h),
                    fontSize = 48.sp,
                    modifier = Modifier
                        .offset(y = ((h - currentHour) * 50 + hourOffset.roundToInt()).dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                hourOffset += dragAmount
                                if (hourOffset > 50) {
                                    currentHour = (currentHour - 1).coerceIn(0, 23)
                                    hourOffset -= 50
                                } else if (hourOffset < -50) {
                                    currentHour = (currentHour + 1).coerceIn(0, 23)
                                    hourOffset += 50
                                }
                                onTimeChange(currentHour, currentMinute)
                            }
                        }
                )
            }
        }

        Text(":", fontSize = 48.sp)

        // Minute wheel
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            (0..59).forEach { m ->
                Text(
                    text = String.format("%02d", m),
                    fontSize = 48.sp,
                    modifier = Modifier
                        .offset(y = ((m - currentMinute) * 50 + minuteOffset.roundToInt()).dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                minuteOffset += dragAmount
                                if (minuteOffset > 50) {
                                    currentMinute = (currentMinute - 1).coerceIn(0, 59)
                                    minuteOffset -= 50
                                } else if (minuteOffset < -50) {
                                    currentMinute = (currentMinute + 1).coerceIn(0, 59)
                                    minuteOffset += 50
                                }
                                onTimeChange(currentHour, currentMinute)
                            }
                        }
                )
            }
        }
    }
}