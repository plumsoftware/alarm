package ru.plumsoftware.alarm.ui.screen

import android.annotation.SuppressLint
import android.app.Activity
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
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.core.content.edit
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
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.plumsoftware.alarm.MyApplication
import ru.plumsoftware.alarm.data.alarmSounds
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.alarmGrayTextColor
import ru.plumsoftware.alarm.ui.theme.alarmRedColor
import java.time.LocalTime
import kotlin.collections.sortedDescending
import ru.plumsoftware.alarm.R
import ru.plumsoftware.alarm.ui.theme.alarmBottomBarColor

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
    var listMode by remember { mutableStateOf(ListMode.MAIN) }

    var isAdsLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = LocalActivity.current

    LaunchedEffect(key1 = Unit) {
        initialize(context) {
            CoroutineScope(Dispatchers.IO).launch {
                val sp = context.getSharedPreferences("alarm_settings", MODE_PRIVATE)
                val count = sp.getInt("count_of_launches", 0)

                if (count <= 2) {
                    sp.edit {
                        putInt("count_of_launches", (count + 1))
                    }
                    return@launch
                } else {
                    withContext(Dispatchers.Main) {
                        isAdsLoading = true
                    }
                    withContext(Dispatchers.Main) {
                        showOpenAds(
                            context = context,
                            activity = activity,
                            onLoaded = {
                                isAdsLoading = false
                            },
                            onFailed = {
                                isAdsLoading = false
                            }
                        )
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    var selectedBottomItemIndex by remember { mutableIntStateOf(0) }

    var selectedTimerSound by remember { mutableStateOf(alarmSounds.first()) } // Звук по умолчанию (Радар)

    LaunchedEffect(Unit) {
        repository.getAllAlarms().collectLatest { list ->
            alarms = list
        }

        coroutineScope.launch {
            if (showBottomSheet) {
                modalSheetState.expand()
            } else {
                modalSheetState.hide()
                selectedAlarm = Alarm(
                    hour = 0,
                    minute = 0
                )
            }
        }
    }

    LaunchedEffect(alarms) {
        if (alarms.isEmpty()) {
            listMode = ListMode.MAIN
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            if (selectedBottomItemIndex == 0)
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    navigationIcon = {
                        SecondaryButton(
                            text = if (listMode == ListMode.MAIN) "Редакировать" else "Готово",
                            onClick = {
                                listMode = when (listMode) {
                                    ListMode.MAIN -> ListMode.DELETING
                                    ListMode.DELETING -> ListMode.MAIN
                                }
                            }
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
        },
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedBottomItemIndex) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Будильники",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Start
                        )
                        // ... (остальной код списка будильников без изменений) ...
                        if (alarms.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Нет будильников. Добавьте новый.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.padding(top = 24.dp)) {
                                items(alarms) { alarm ->
                                    Column {
                                        AlarmItem(
                                            listMode = listMode,
                                            alarm = alarm,
                                            onToggle = { enabled ->
                                                val updated = alarm.copy(isEnabled = enabled)
                                                coroutineScope.launch {
                                                    repository.update(updated)
                                                    if (enabled) AlarmManagerHelper.setAlarm(
                                                        context,
                                                        updated
                                                    )
                                                    else AlarmManagerHelper.cancelAlarm(
                                                        context,
                                                        updated
                                                    )
                                                }
                                            },
                                            onEdit = {
                                                sheetRoutes = SheetRoutes.Main
                                                selectedAlarm = alarm
                                                showBottomSheet = true
                                            },
                                            onDelete = {
                                                coroutineScope.launch {
                                                    repository.delete(alarm)
                                                    AlarmManagerHelper.cancelAlarm(context, alarm)
                                                    repository.getAllAlarms()
                                                        .collectLatest { list ->
                                                            withContext(Dispatchers.Main) {
                                                                alarms = list
                                                            }
                                                        }
                                                }
                                            }
                                        )
                                        if (alarms.last().id == alarm.id) Spacer(
                                            modifier = Modifier.height(
                                                120.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SecScreen()
                    }
                }

                2 -> {
                    // --- NEW TIMER SCREEN ---
                    // Таймер
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimerScreen(
                            selectedSound = selectedTimerSound,
                            onSoundClick = {
                                sheetRoutes = SheetRoutes.TimerSounds
                                showBottomSheet = true
                            }
                        )
                    }
                }
            }

            if (isAdsLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f))
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = primaryColor,
                    )
                }
            }

            // --- BOTTOM BAR (СЛОЙ 1: РАЗМЫТЫЙ ФОН) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp, top = 12.dp)
                    .background(color = alarmBottomBarColor.copy(alpha = 0.95f))
                    .blur(50.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = getNavigationBarHeight(), top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly // Равномерно для 3 элементов
                ) {
                    // 1. Будильник (Фон)
                    Column(
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .clickable(true) { selectedBottomItemIndex = 0 },
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.alarm),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (selectedBottomItemIndex == 0) primaryColor else alarmGrayTextColor),
                        )
                        Text(
                            text = "Будильник",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 0) primaryColor else alarmGrayTextColor)
                        )
                    }

                    // 2. Секундомер (Фон)
                    Column(
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .clickable(true) { selectedBottomItemIndex = 1 },
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Rounded.WatchLater,
                            contentDescription = null,
                            tint = if (selectedBottomItemIndex == 1) primaryColor else alarmGrayTextColor
                        )
                        Text(
                            text = "Секундомер",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 1) primaryColor else alarmGrayTextColor)
                        )
                    }

                    // 3. Таймер (Фон) - НОВОЕ
                    Column(
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .clickable(true) { selectedBottomItemIndex = 2 },
                        verticalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Icons.Rounded.Timer, // Убедись, что импортировал androidx.compose.material.icons.rounded.Timer
                            contentDescription = null,
                            tint = if (selectedBottomItemIndex == 2) primaryColor else alarmGrayTextColor
                        )
                        Text(
                            text = "Таймер",
                            style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 2) primaryColor else alarmGrayTextColor)
                        )
                    }
                }
            }

            // --- BOTTOM BAR (СЛОЙ 2: КЛИКАБЕЛЬНЫЙ ПЕРЕДНИЙ ПЛАН) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = getNavigationBarHeight(), top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly // Равномерно для 3 элементов
            ) {
                // 1. Будильник (Кликабельный)
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .background(Color.Transparent)
                        .clickable(
                            enabled = true,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedBottomItemIndex = 0 },
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.alarm),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (selectedBottomItemIndex == 0) primaryColor else alarmGrayTextColor),
                    )
                    Text(
                        text = "Будильник",
                        style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 0) primaryColor else alarmGrayTextColor)
                    )
                }

                // 2. Секундомер (Кликабельный)
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedBottomItemIndex = 1 },
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.WatchLater,
                        contentDescription = null,
                        tint = if (selectedBottomItemIndex == 1) primaryColor else alarmGrayTextColor
                    )
                    Text(
                        text = "Секундомер",
                        style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 1) primaryColor else alarmGrayTextColor)
                    )
                }

                // 3. Таймер (Кликабельный) - НОВОЕ
                Column(
                    modifier = Modifier
                        .padding(all = 8.dp)
                        .clickable(
                            enabled = true,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedBottomItemIndex = 2 },
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = if (selectedBottomItemIndex == 2) primaryColor else alarmGrayTextColor
                    )
                    Text(
                        text = "Таймер",
                        style = MaterialTheme.typography.bodySmall.copy(color = if (selectedBottomItemIndex == 2) primaryColor else alarmGrayTextColor)
                    )
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
                        minute = now.get(java.util.Calendar.MINUTE),
                        repeatDays = listOf(0)
                    ) else selectedAlarm
            )
        }
        var repeat by remember {
            mutableStateOf<RepeatAlarm>(
                RepeatAlarm.fromStringTo(
                    alarm.repeatDays.sortedDescending().joinToString { dayToString(it) })
            )
        }
        val alarmManager =
            remember { context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager }
        var selectedSoundItem by remember {
            mutableStateOf(alarmSounds.firstOrNull { it.second == selectedAlarm.sound }
                ?: alarmSounds[16])
        }

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
            contentWindowInsets = { WindowInsets(0, 64, 0, 0) },
            dragHandle = null,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
        ) {

            val view = LocalView.current
            val context = LocalContext.current
            val window = LocalActivity.current?.window
            val resources = LocalActivity.current?.resources

            // Initial hiding of the status bar
            SideEffect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.windowInsetsController?.hide(android.view.WindowInsets.Type.statusBars())
                    view.windowInsetsController?.hide(android.view.WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    view.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                }
            }

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
                                    selectedAlarm = Alarm(
                                        hour = 0,
                                        minute = 0
                                    )
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
                                                repository.insert(alarm.copy(isEnabled = true))
                                                alarm  // Note: id is auto-generated, but for simplicity, assume we refetch or update
                                            } else {
                                                repository.update(
                                                    alarm.copy(
                                                        id = selectedAlarm.id,
                                                        isEnabled = true
                                                    )
                                                )
                                                alarm.copy(id = selectedAlarm.id, isEnabled = true)
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
                                            (repeat as RepeatAlarm.Days).list.map { it.id }
                                                .sortedDescending().joinToString { dayToString(it) }
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

                SheetRoutes.TimerSounds -> {
                    AlarmSoundSheet(
                        item = selectedTimerSound,
                        onSelected = {
                            selectedTimerSound = it
                            // Мы не закрываем шторку сразу, чтобы пользователь мог послушать,
                            // но можно и закрыть: showBottomSheet = false
                        },
                        onBack = {
                            // Для таймера кнопка "Назад" просто закрывает шторку,
                            // так как нет предыдущего экрана меню
                            showBottomSheet = false
                        }
                    )
                }
            }
        }
    }
}

private fun showOpenAds(context: Context, activity: Activity?, onLoaded: () -> Unit, onFailed: () -> Unit) {
    var mAppOpenAd: AppOpenAd?
    val appOpenAdLoader = AppOpenAdLoader(context)
    val AD_UNIT_ID: String = MyApplication.adsConfig.OPEN_MAIN_SCREEN_AD
    val adRequestConfiguration = AdRequestConfiguration.Builder(AD_UNIT_ID).build()

    val appOpenAdEventListener: AppOpenAdEventListener = object : AppOpenAdEventListener {
        override fun onAdShown() {
            onLoaded()
        }

        override fun onAdDismissed() {}

        override fun onAdFailedToShow(adError: AdError) {
            onFailed()
        }

        override fun onAdClicked() {}

        override fun onAdImpression(@Nullable impressionData: ImpressionData?) {}
    }

    val appOpenAdLoadListener: AppOpenAdLoadListener = object : AppOpenAdLoadListener {

        override fun onAdFailedToLoad(error: AdRequestError) {
            onFailed()
        }

        override fun onAdLoaded(appOpenAd: AppOpenAd) {
            mAppOpenAd = appOpenAd
            mAppOpenAd.setAdEventListener(appOpenAdEventListener)
            if (activity != null)
                mAppOpenAd.show(activity)
        }
    }

    appOpenAdLoader.setAdLoadListener(appOpenAdLoadListener)
    appOpenAdLoader.loadAd(adRequestConfiguration)
}

@Composable
fun getNavigationBarHeight(): Dp {
    val density = LocalDensity.current
    val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density)
    return with(density) { navigationBarHeightPx.toDp() }
}

@Composable
fun AlarmItem(
    alarm: Alarm,
    listMode: ListMode,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: (Alarm) -> Unit
) {
    // Одна анимация прогресса для всего!
    val deletionProgress = remember { Animatable(0f) }

    LaunchedEffect(listMode) {
        val target = if (listMode == ListMode.DELETING) 1f else 0f
        if (deletionProgress.value != target) {
            deletionProgress.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    // Вычисляем все параметры на основе одного прогресса
    val switchOffset = lerp(0f, 100f, deletionProgress.value)
    val switchAlpha = lerp(1f, 0f, deletionProgress.value)
    val deleteOffset = lerp(-100f, 0f, deletionProgress.value)
    val deleteAlpha = lerp(0f, 1f, deletionProgress.value)
    val contentStartPadding = lerp(0.dp, 44.dp, deletionProgress.value)

    // Анимации, зависящие от alarm.isEnabled — оставляем как есть (они не связаны с listMode)
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
            // Кнопка удаления
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = deleteOffset
                        alpha = deleteAlpha
                    }
            ) {
                if (deletionProgress.value > 0.01f) { // Показываем, если почти началась анимация
                    IconButton(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = alarmRedColor
                        ),
                        onClick = { onDelete(alarm) },
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(4.dp)
                                .background(Color.White)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = contentStartPadding)
            ) {
                Text(
                    text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.titleMedium.copy(color = textColor)
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

            // Переключатель
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(28.dp)
                    .graphicsLayer {
                        translationX = switchOffset
                        alpha = switchAlpha
                    }
            ) {
                if (deletionProgress.value < 0.99f) { // Скрываем, если почти закончилась анимация
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
    }
}

fun dayToString(day: Int): String {
    return when (day) {
        0 -> "Никогда"
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
    data object TimerSounds : SheetRoutes()
}

enum class ListMode {
    MAIN, DELETING
}