package ru.plumsoftware.alarm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.commandiron.wheel_picker_compose.WheelTimePicker
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AlarmRepository
import ru.plumsoftware.alarm.ui.Constants
import ru.plumsoftware.alarm.ui.components.SecondaryButton
import ru.plumsoftware.alarm.ui.theme.primaryColor
import ru.plumsoftware.alarm.ui.theme.switchCheckedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(navController: NavController, context: Context) {
    val repository = remember { AlarmRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var alarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getAllAlarms().collectLatest { list ->
            alarms = list
        }

        coroutineScope.launch {
            if (showBottomSheet) {
                modalSheetState.expand()
            } else {
                modalSheetState.hide()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    SecondaryButton(
                        text = "Редакировать",
                        onClick = {}
                    )
                },
                title = {},
                actions = {
                    IconButton(
                        onClick = {
                            showBottomSheet = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = primaryColor
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Будильники",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start
            )
            if (alarms.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет будильников. Добавьте новый.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(top = 24.dp)
                ) {
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

    if (showBottomSheet) {
        ModalBottomSheet(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 48.dp),
            onDismissRequest = { showBottomSheet = false },
            sheetState = modalSheetState,
            contentWindowInsets = {
                BottomSheetDefaults.windowInsets
                    .add(WindowInsets.navigationBars)
                    .add(WindowInsets.statusBars)
            },
            dragHandle = null,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    space = 16.dp,
                    alignment = Alignment.Top
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 4.dp,
                        alignment = Alignment.CenterHorizontally
                    )
                ) {
                    Text(
                        modifier = Modifier.clickable(true) {
                            showBottomSheet = false
                        },
                        text = "Отменить",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Добавить будильник",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        modifier = Modifier.clickable(true) {
                            showBottomSheet = false
                        },
                        text = "Сохранить",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                WheelTimePicker { snappedTime ->

                }
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    val trackColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) switchCheckedColor else Color.White.copy(0.1f),
        animationSpec = tween(durationMillis = Constants.SWITCH_ANIM_DELAY),
        label = "trackColorAnimation"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (alarm.isEnabled) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = Constants.SWITCH_ANIM_DELAY),
        label = "thumbOffsetAnimation"
    )

    val textColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) Color.White else Color.White.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = Constants.SWITCH_ANIM_DELAY),
        label = "textColorAnimation"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.Start
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth().clip(CircleShape),
            thickness = 1.dp,
            color = DividerDefaults.color
        )
        Row(
            modifier = Modifier.wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textColor
                    )
                )
                if (alarm.label.isNotEmpty())
                    Text(
                        text = alarm.label,
                        color = Color.Gray
                    )
                if (alarm.repeatDays.isNotEmpty())
                    Text(
                        text = alarm.repeatDays.joinToString { dayToString(it) },
                        color = Color.Gray
                    )

                Text(
                    text = "Будильник",
                    style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                )
            }

            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(trackColor)
                    .clickable { onToggle(!alarm.isEnabled) },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffset)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
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