package ru.plumsoftware.alarm.ui.screen

import android.annotation.SuppressLint
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
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import com.commandiron.wheel_picker_compose.WheelTimePicker
import com.commandiron.wheel_picker_compose.core.WheelPickerDefaults
import ru.plumsoftware.alarm.data.Alarm
import ru.plumsoftware.alarm.data.AlarmManagerHelper
import ru.plumsoftware.alarm.data.AlarmRepository
import ru.plumsoftware.alarm.ui.Constants
import ru.plumsoftware.alarm.ui.components.SecondaryButton
import ru.plumsoftware.alarm.ui.theme.primaryColor
import ru.plumsoftware.alarm.ui.theme.switchCheckedColor
import androidx.core.net.toUri
import ru.plumsoftware.alarm.data.alarmSounds
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmGrayTextColor
import ru.plumsoftware.alarm.ui.theme.alarmRedColor
import java.time.LocalTime
import kotlin.collections.sortedDescending

@SuppressLint("ConfigurationScreenWidthHeight")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(navController: NavController, context: Context) {
    val repository = remember { AlarmRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var alarms by remember { mutableStateOf<List<Alarm>>(emptyList()) }
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden
        }
    )
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedAlarm by remember { mutableStateOf(Alarm(hour = 0, minute = 0)) }
    var sheetRoutes by remember { mutableStateOf(SheetRoutes()) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

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
                            sheetRoutes = SheetRoutes.Main
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
                            sheetRoutes = SheetRoutes.Main
                            selectedAlarm = alarm
                            showBottomSheet = true
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
        var alarmName by remember {
            mutableStateOf(
                if (selectedAlarm.id == 0) "" else selectedAlarm.label
            )
        }
        val repository = remember { AlarmRepository(context) }
        val coroutineScope = rememberCoroutineScope()
        val now = Calendar.getInstance()
        var alarm by remember {
            mutableStateOf(
                if (selectedAlarm.id == 0)
                    Alarm(
                        hour = now.get(java.util.Calendar.HOUR_OF_DAY),
                        minute = now.get(java.util.Calendar.MINUTE)
                    ) else selectedAlarm
            )
        }
        var repeat by remember { mutableStateOf<RepeatAlarm>(RepeatAlarm.Never()) }
        val alarmManager =
            remember { context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager }
        var selectedSoundItem by remember { mutableStateOf(alarmSounds[16]) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* After returning from settings, can check again if needed */ }

        val trackColor by animateColorAsState(
            targetValue = if (alarm.snoozeEnabled) switchCheckedColor else Color.White.copy(0.1f),
            animationSpec = tween(durationMillis = Constants.SWITCH_ANIM_DELAY),
            label = "trackColorAnimation"
        )

        val thumbOffset by animateDpAsState(
            targetValue = if (alarm.snoozeEnabled) 18.dp else 0.dp,
            animationSpec = tween(durationMillis = Constants.SWITCH_ANIM_DELAY),
            label = "thumbOffsetAnimation"
        )

        ModalBottomSheet(
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 48.dp),
            onDismissRequest = { showBottomSheet = false },
            sheetState = modalSheetState,
            contentWindowInsets = {
                BottomSheetDefaults.windowInsets
//                    .add(WindowInsets.navigationBars)
                    .add(WindowInsets.statusBars)
            },
            dragHandle = null,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
        ) {
            when (sheetRoutes) {
                SheetRoutes.Main -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(
                            space = 24.dp,
                            alignment = Alignment.Top
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
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
                                    val canSchedule =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            alarmManager.canScheduleExactAlarms()
                                        } else true
                                    if (canSchedule) {
                                        coroutineScope.launch {
                                            val savedAlarm = if (selectedAlarm.id == 0) {
                                                repository.insert(alarm)
                                                alarm  // Note: id is auto-generated, but for simplicity, assume we refetch or update
                                            } else {
                                                repository.update(alarm.copy(id = selectedAlarm.id))
                                                alarm.copy(id = selectedAlarm.id)
                                            }
                                            AlarmManagerHelper.setAlarm(context, savedAlarm)
                                            navController.popBackStack()
                                        }.invokeOnCompletion {
                                            showBottomSheet = false
                                        }
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent =
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                    data = "package:${context.packageName}".toUri()
                                                }
                                            launcher.launch(intent)
                                        }
                                    }
                                },
                                text = "Сохранить",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        WheelTimePicker(
                            size = DpSize((screenWidthDp.value - 32).dp, 250.dp),
                            textStyle = MaterialTheme.typography.titleSmall,
                            startTime = if (selectedAlarm.id != 0) LocalTime.of(
                                selectedAlarm.hour,
                                selectedAlarm.minute
                            ) else LocalTime.of(alarm.hour, alarm.minute),
                            textColor = Color.White,
                            selectorProperties = WheelPickerDefaults.selectorProperties(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(width = 0.dp, color = Color.Transparent)
                            )
                        ) { snappedTime ->
                            alarm = alarm.copy(hour = snappedTime.hour, minute = snappedTime.minute)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topEnd = 10.dp,
                                            topStart = 10.dp,
                                            bottomEnd = 0.dp,
                                            bottomStart = 0.dp
                                        )
                                    )
                                    .background(
                                        alarmCardColor, RoundedCornerShape(
                                            topEnd = 10.dp,
                                            topStart = 10.dp,
                                            bottomEnd = 0.dp,
                                            bottomStart = 0.dp
                                        )
                                    )
                                    .clickable(enabled = true) {
                                        sheetRoutes = SheetRoutes.Repeat
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    ),
                                    text = "Повтор",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .wrapContentSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        space = 4.dp,
                                        alignment = Alignment.CenterHorizontally
                                    )
                                ) {
                                    val title = when (repeat) {
                                        is RepeatAlarm.Days -> {
                                            (repeat as RepeatAlarm.Days).list.map {it.id}.sortedDescending().joinToString { dayToString(it) }
                                        }
                                        is RepeatAlarm.Never -> {
                                            (repeat as RepeatAlarm.Never).title
                                        }
                                    }
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = alarmGrayTextColor
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = alarmGrayTextColor
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        alarmCardColor, RoundedCornerShape(
                                            topEnd = 10.dp,
                                            topStart = 10.dp,
                                            bottomEnd = 0.dp,
                                            bottomStart = 0.dp
                                        )
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        alarmCardColor, RectangleShape
                                    )
                                    .clickable(enabled = true) {},
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    ),
                                    text = "Название",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .wrapContentSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BasicTextField(
                                        value = alarmName,
                                        maxLines = 1,
                                        onValueChange = {
                                            alarmName = it
                                            alarm = alarm.copy(label = alarmName)
                                        },
                                        modifier = Modifier.wrapContentSize(),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = alarmGrayTextColor,
                                            textAlign = TextAlign.End
                                        ),
                                        cursorBrush = SolidColor(primaryColor),
                                        decorationBox = { innerTextField ->
                                            Box(
                                                modifier = Modifier
                                                    .wrapContentSize(align = Alignment.CenterEnd)
                                                    .padding(end = 8.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                val trailingPadding =
                                                    if (alarmName.isNotEmpty()) 20.dp else 0.dp
                                                Box(
                                                    modifier = Modifier.padding(end = trailingPadding)
                                                ) {
                                                    innerTextField()
                                                }

                                                if (alarmName.isEmpty()) {
                                                    Text(
                                                        text = "Будильник",
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            color = alarmGrayTextColor.copy(alpha = 0.5f),
                                                            textAlign = TextAlign.End
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(end = if (alarmName.isEmpty()) 0.dp else 8.dp)
                                                    )
                                                }

                                                if (alarmName.isNotEmpty()) {
                                                    IconButton(
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clip(CircleShape),
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            containerColor = alarmGrayTextColor.copy(
                                                                alpha = 0.5f
                                                            ),
                                                            contentColor = alarmCardColor
                                                        ),
                                                        onClick = { alarmName = "" }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Clear,
                                                            contentDescription = null
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        alarmCardColor, RoundedCornerShape(
                                            topEnd = 10.dp,
                                            topStart = 10.dp,
                                            bottomEnd = 0.dp,
                                            bottomStart = 0.dp
                                        )
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(
                                        alarmCardColor, RectangleShape
                                    )
                                    .clickable(enabled = true) {
                                        sheetRoutes = SheetRoutes.Sounds
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    ),
                                    text = "Мелодия",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .wrapContentSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 0.dp, vertical = 0.dp)
                                            .wrapContentSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(
                                            space = 4.dp,
                                            alignment = Alignment.CenterHorizontally
                                        )
                                    ) {
                                        Text(
                                            text = selectedSoundItem.first,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = alarmGrayTextColor
                                            )
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = alarmGrayTextColor
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        alarmCardColor, RoundedCornerShape(
                                            topEnd = 10.dp,
                                            topStart = 10.dp,
                                            bottomEnd = 0.dp,
                                            bottomStart = 0.dp
                                        )
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topEnd = 0.dp,
                                            topStart = 0.dp,
                                            bottomEnd = 10.dp,
                                            bottomStart = 10.dp
                                        )
                                    )
                                    .background(
                                        alarmCardColor, RoundedCornerShape(
                                            topEnd = 0.dp,
                                            topStart = 0.dp,
                                            bottomEnd = 10.dp,
                                            bottomStart = 10.dp
                                        )
                                    )
                                    .clickable(enabled = true) {

                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            )
                            {
                                Text(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 12.dp
                                    ),
                                    text = "Повторение сигнала",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White
                                    )
                                )

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .wrapContentSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        space = 4.dp,
                                        alignment = Alignment.CenterHorizontally
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(28.dp)
                                            .clip(CircleShape)
                                            .background(trackColor)
                                            .clickable {
                                                alarm =
                                                    alarm.copy(snoozeEnabled = !alarm.snoozeEnabled)
                                            },
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

                            if (alarm.id != 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = alarmCardColor,
                                        contentColor = alarmRedColor
                                    ),
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.delete(alarm)
                                            AlarmManagerHelper.cancelAlarm(context, alarm)

                                            showBottomSheet = false
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp, vertical = 12.dp)
                                                .align(Alignment.Center),
                                            text = "Удалить будильник",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = alarmRedColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SheetRoutes.Sounds -> {
                    AlarmSoundSheet(
                        item = selectedSoundItem,
                        onSelected = {
                            selectedSoundItem = it
                            alarm = alarm.copy(sound = it.second)
                        },
                        onBack = {
                            sheetRoutes = SheetRoutes.Main
                        }
                    )
                }

                SheetRoutes.Repeat -> {
                    AlarmRepeatSheet(
                        item = repeat,
                        onSelected = {
                            repeat = it

                            alarm = when (repeat) {
                                is RepeatAlarm.Days -> {
                                    alarm.copy(repeatDays = (repeat as RepeatAlarm.Days).list.map { it.id })
                                }

                                is RepeatAlarm.Never -> {
                                    alarm.copy(repeatDays = listOf())
                                }
                            }
                        },
                        onBack = {
                            sheetRoutes = SheetRoutes.Main
                        }
                    )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clickable(true) {
                onEdit()
            },
        verticalArrangement = Arrangement.spacedBy(
            space = 8.dp,
            alignment = Alignment.CenterVertically
        ),
        horizontalAlignment = Alignment.Start
    ) {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape),
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
                if (alarm.repeatDays.isNotEmpty())
                    Text(
                        text = alarm.repeatDays.joinToString { dayToString(it) },
                        color = Color.Gray
                    )

                Text(
                    text = alarm.label.ifEmpty { "Будильник" },
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
        1 -> "Пн"
        2 -> "Вт"
        3 -> "Ср"
        4 -> "Чт"
        5 -> "Пт"
        6 -> "Сб"
        7 -> "Вс"
        else -> ""
    }
}

open class SheetRoutes {
    data object Main : SheetRoutes()
    data object Sounds : SheetRoutes()
    data object Repeat : SheetRoutes()
}