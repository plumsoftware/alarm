package ru.plumsoftware.alarm.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.plumsoftware.alarm.ui.theme.alarmCardColor
import ru.plumsoftware.alarm.ui.theme.primaryColor

@Composable
fun AlarmRepeatSheet(
    item: RepeatAlarm,
    onSelected: (RepeatAlarm) -> Unit,
    onBack: () -> Unit,
    context: Context = LocalContext.current
) {
    var selectedItem by remember { mutableStateOf(item) }
    var isNever by remember { mutableStateOf(false) }
    val days = remember { mutableListOf<Int>() }

    if (isNever) {
        days.clear()
    }

    LaunchedEffect(Unit) {
        if (item is RepeatAlarm.Days) {
            item.list.forEach {
                days.add(it.id)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 0.dp,
            alignment = Alignment.Top
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )
        {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .wrapContentSize()
                    .clickable(true) {
                        onBack()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = primaryColor
                )
                Text(
                    text = "Назад",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = primaryColor
                    )
                )
            }
            Text(
                modifier = Modifier
                    .align(Alignment.Center),
                text = "Повтор",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                    selectedItem = RepeatAlarm.Never()
                    isNever = true
                    onSelected(selectedItem)
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
                text = "Никогда",
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
                if (selectedItem is RepeatAlarm.Never) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(1)) {
                        days.remove(1)
                    } else {
                        days.add(1)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждый понедельник",
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
                if (days.contains(1)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(2)) {
                        days.remove(2)
                    } else {
                        days.add(2)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждый вторник",
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
                if (days.contains(2)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(3)) {
                        days.remove(3)
                    } else {
                        days.add(3)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждую среду",
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
                if (days.contains(3)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(4)) {
                        days.remove(4)
                    } else {
                        days.add(4)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждый четверг",
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
                if (days.contains(4)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(5)) {
                        days.remove(5)
                    } else {
                        days.add(5)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждую пятницу",
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
                if (days.contains(5)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                .background(
                    alarmCardColor, RectangleShape
                )
                .clickable(enabled = true) {
                    isNever = false
                    if (days.contains(6)) {
                        days.remove(6)
                    } else {
                        days.add(6)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждую субботу",
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
                if (days.contains(6)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
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
                    isNever = false
                    if (days.contains(7)) {
                        days.remove(7)
                    } else {
                        days.add(7)
                    }
                    selectedItem = RepeatAlarm.Days(list = days.map { RepeatAlarm.Day(id = it) })
                    onSelected(selectedItem)
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
                text = "Каждое воскресенье",
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
                if (days.contains(7)) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = primaryColor
                    )
                }
            }
        }
    }
}

sealed class RepeatAlarm {
    data class Never(val title: String = "Никогда", val id: Int = 0) : RepeatAlarm()
    data class Day(
        val id: Int
    )

    data class Days(val list: List<Day>) : RepeatAlarm()
}